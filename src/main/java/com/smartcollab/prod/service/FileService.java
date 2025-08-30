package com.smartcollab.prod.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.smartcollab.prod.dto.*;
import com.smartcollab.prod.entity.*;
import com.smartcollab.prod.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// Azure Blob Storage를 사용하여 파일 업로드, 다운로드, 버전 관리 및 삭제를 처리하는 서비스.
@Service
@RequiredArgsConstructor
public class FileService {

    // Azure Blob Storage와 통신하기 위한 클라이언트 (자동으로 주입됩니다)
    private final BlobContainerClient blobContainerClient;

    // 데이터베이스 접근을 위한 Repository
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final TeamRepository teamRepository;
    private final FileVersionRepository fileVersionRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final SignatureRepository signatureRepository;

    // 서비스 간 로직 호출을 위한 다른 서비스
    private final SignatureService signatureService;

    /**
     * 파일을 Azure Blob Storage에 업로드하고, 메타데이터를 DB에 저장
     *
     * @param file             업로드할 MultipartFile
     * @param username         업로드하는 사용자 이름
     * @param folderId         파일이 저장될 부모 폴더 ID (개인 드라이브)
     * @param teamId           파일이 저장될 팀 ID (팀 스토리지)
     * @return 생성된 파일 엔티티
     * @throws IOException 파일 처리 중 예외 발생 시
     */
    @Transactional
    public FileEntity uploadFile(MultipartFile file, String username, Long folderId, Long teamId) throws IOException {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Folder parentFolder = findParentFolder(folderId, teamId);

        checkFolderPermission(parentFolder, owner);

        String originalName = file.getOriginalFilename();
        String storedName = UUID.randomUUID().toString() + "_" + originalName;

        //  Azure Blob Storage에 파일 업로드
        BlobClient blobClient = blobContainerClient.getBlobClient(storedName);
        try (InputStream inputStream = file.getInputStream()) {
            blobClient.upload(inputStream, file.getSize(), true); // true: 덮어쓰기 허용
        }

        FileEntity fileEntity = FileEntity.builder()
                .owner(owner)
                .folder(parentFolder)
                .originalName(originalName)
                .storedName(storedName) // Blob 이름을 DB에 저장
                .size(file.getSize())
                .build();
        FileEntity savedFile = fileRepository.save(fileEntity);

        // 파일의 첫 번째 버전을 생성. 원본 파일의 Blob 이름을 그대로 사용
        FileVersion initialVersion = FileVersion.builder()
                .file(savedFile)
                .storedPath(savedFile.getStoredName())
                .editor(owner)
                .build();
        fileVersionRepository.save(initialVersion);

        // 생성된 초기 버전을 파일의 '활성 버전'으로 지정
        savedFile.setActiveVersion(initialVersion);
        return savedFile;
    }

    /**
     * 텍스트 파일의 내용을 수정하고, 새 버전을 Azure Blob Storage에 저장
     *
     * @param fileId  수정할 파일의 ID
     * @param content 새로운 파일 내용
     * @param username 수정하는 사용자 이름
     * @throws IOException 파일 처리 중 예외 발생 시
     */
    @Transactional
    public void saveNewVersion(Long fileId, String content, String username) throws IOException {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        FileEntity file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        checkFileEditPermission(file, user);
        signatureService.invalidateSignatures(file); // 기존 서명 무효화

        // 버전 관리를 위해 별도의 경로(prefix)에 저장
        String versionStoredName = "versions/" + UUID.randomUUID().toString() + ".txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        // Azure Blob Storage에 새 버전 파일 업로드
        BlobClient blobClient = blobContainerClient.getBlobClient(versionStoredName);
        blobClient.upload(new ByteArrayResource(contentBytes).getInputStream(), contentBytes.length, true);

        FileVersion newVersion = FileVersion.builder()
                .file(file)
                .storedPath(versionStoredName) // 새 버전의 Blob 이름을 저장
                .editor(user)
                .build();
        fileVersionRepository.save(newVersion);

        // 새로 저장된 버전을 파일의 '활성 버전'으로 업데이트
        file.setActiveVersion(newVersion);
        file.setSize((long) contentBytes.length);
        fileRepository.save(file);
    }

    /**
     * 파일의 '활성 버전'을 Azure Blob Storage에서 다운로드
     *
     * @param fileId 다운로드할 파일의 ID
     * @return 다운로드할 파일의 Resource 객체
     */
    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId) {
        FileEntity file = fileRepository.findByIdWithActiveVersion(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));
        FileVersion activeVersion = getActiveVersion(file);

        String blobName = activeVersion.getStoredPath();

        // Azure Blob Storage에서 파일 다운로드
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        if (!blobClient.exists()) {
            throw new RuntimeException("Azure Storage에서 파일을 찾을 수 없습니다: " + blobName);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);

        final String downloadName = file.getOriginalName(); // 위에서 조회한 FileEntity의 원본명
        return new ByteArrayResource(outputStream.toByteArray()) {
            @Override
            public String getFilename() {
                return downloadName;
            }
        };
    }

    /**
     * 파일을 DB와 Azure Blob Storage에서 영구적으로 삭제
     * 원본 파일과 모든 버전 파일을 함께 삭제
     * @param fileId   삭제할 파일 ID
     * @param username 삭제를 요청한 사용자 이름
     * @throws IOException 예외
     */
    @Transactional
    public void deleteFilePermanently(Long fileId, String username) throws IOException {
        FileEntity fileEntity = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        checkPermanentDeletePermission(fileEntity, user);

        // 연결된 공유 링크가 있다면 먼저 삭제합니다.
        shareLinkRepository.deleteAll(shareLinkRepository.findByFile(fileEntity));

        // --- Azure Blob Storage에서 모든 버전 파일 및 원본 파일 삭제 ---
        for (FileVersion version : fileEntity.getVersions()) {
            blobContainerClient.getBlobClient(version.getStoredPath()).deleteIfExists();
        }
        blobContainerClient.getBlobClient(fileEntity.getStoredName()).deleteIfExists();

        // DB에서 파일 정보를 최종적으로 삭제합니다.
        fileRepository.delete(fileEntity);
    }

    /**
     * 파일의 '활성 버전' 내용을 텍스트로 읽어옵니다.
     * @param fileId 파일 ID
     * @return 파일 내용 문자열
     * @throws IOException 예외
     */
    @Transactional(readOnly = true)
    public String getLatestFileContent(Long fileId) throws IOException {
        FileEntity file = fileRepository.findByIdWithActiveVersion(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        FileVersion activeVersion = getActiveVersion(file);

        String blobName = activeVersion.getStoredPath();
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        if (!blobClient.exists()) {
            return "활성 버전(" + activeVersion.getVersionId() + ")의 실제 파일을 찾을 수 없습니다.";
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }


    @Transactional
    public void restoreVersion(Long fileId, Long versionId, String username) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        checkFileEditPermission(file, user);
        FileVersion versionToRestore = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("복원할 버전을 찾을 수 없습니다."));

        file.setActiveVersion(versionToRestore);

        // 복원된 버전의 파일 크기로 업데이트
        String blobName = versionToRestore.getStoredPath();
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        if(blobClient.exists()){
            file.setSize(blobClient.getProperties().getBlobSize());
        }

        fileRepository.save(file);
        signatureService.invalidateSignatures(file);
    }

    @Transactional(readOnly = true)
    public List<VersionHistoryDto> getVersionHistory(Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        return file.getVersions().stream()
                .map(version -> new VersionHistoryDto(version, signatureRepository.findByFile(file)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void renameFile(Long fileId, String newName, String username) {
        FileEntity fileEntity = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        checkFileEditPermission(fileEntity, user);
        fileEntity.changeName(newName);
        fileRepository.save(fileEntity);
    }

    @Transactional
    public void moveFileToTrash(Long fileId, String username) {
        FileEntity fileEntity = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        checkFileDeletePermission(fileEntity, user);
        fileEntity.moveToTrash();
        fileRepository.save(fileEntity);
    }

    /**
     * @param username 사용자 이름
     * @return 휴지통의 파일 목록
     */
    @Transactional(readOnly = true)
    public List<FileResponseDto> getFilesInTrash(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return fileRepository.findByOwnerAndIsDeletedTrue(user).stream()
                .map(FileResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * @param fileId 복원할 파일 ID
     * @param username 사용자 이름
     */
    @Transactional
    public void restoreFileFromTrash(Long fileId, String username) {
        FileEntity fileEntity = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!fileEntity.getOwner().getUserId().equals(user.getUserId())) {
            throw new SecurityException("파일 복원 권한이 없습니다.");
        }
        fileEntity.restoreFromTrash();
        fileRepository.save(fileEntity);
    }

    @Transactional
    public void moveItems(List<String> itemUniqueKeys, Long destinationFolderId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Folder destination = folderRepository.findById(destinationFolderId)
                .orElseThrow(() -> new IllegalArgumentException("이동할 대상 폴더를 찾을 수 없습니다."));
        checkFolderPermission(destination, user);

        for (String key : itemUniqueKeys) {
            String[] parts = key.split("-");
            String type = parts[0];
            Long id = Long.parseLong(parts[1]);
            if ("folder".equals(type)) {
                Folder folder = folderRepository.findById(id).orElseThrow();
                if (destination.getFolderId().equals(folder.getFolderId())) {
                    throw new IllegalArgumentException("폴더를 자기 자신에게 이동할 수 없습니다.");
                }
                folder.setParentFolder(destination);
                folderRepository.save(folder);
            } else {
                FileEntity file = fileRepository.findById(id).orElseThrow();
                file.setFolder(destination);
                fileRepository.save(file);
            }
        }
    }

    @Transactional
    public void copyFiles(List<String> itemUniqueKeys, Long destinationFolderId, String username) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Folder destination = folderRepository.findById(destinationFolderId)
                .orElseThrow(() -> new IllegalArgumentException("복사할 대상 폴더를 찾을 수 없습니다."));
        checkFolderPermission(destination, user);

        for (String key : itemUniqueKeys) {
            String[] parts = key.split("-");
            if ("file".equals(parts[0])) {
                Long id = Long.parseLong(parts[1]);
                FileEntity originalFile = fileRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("원본 파일을 찾을 수 없습니다."));

                checkFileReadPermission(originalFile, user);

                String newStoredName = UUID.randomUUID().toString() + "_" + originalFile.getOriginalName();

                // --- Azure Blob 간 복사 ---
                BlobClient sourceBlob = blobContainerClient.getBlobClient(originalFile.getStoredName());
                BlobClient destBlob = blobContainerClient.getBlobClient(newStoredName);

                // 1. 원본 파일에 대한 임시 읽기 전용 URL(SAS 토큰 포함)을 생성
                String sourceSasUrl = generateReadOnlySasUrl(originalFile.getStoredName(), Duration.ofMinutes(5));
                // 2. SAS URL을 사용하여 안전하게 파일을 복사
                destBlob.copyFromUrl(sourceSasUrl);

                FileEntity copiedFile = FileEntity.builder()
                        .owner(user)
                        .folder(destination)
                        .originalName(originalFile.getOriginalName())
                        .storedName(newStoredName)
                        .size(originalFile.getSize())
                        .build();
                fileRepository.save(copiedFile);
            } else {
                throw new IllegalArgumentException("폴더 복사는 현재 지원되지 않습니다. 파일만 선택해주세요.");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<FileSearchResultDto> searchFiles(String query, Long teamId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        List<Folder> rootFolders;
        if (teamId != null) {
            Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
            rootFolders = folderRepository.findByTeamAndParentFolderIsNull(team);
        } else {
            rootFolders = folderRepository.findByOwnerAndTeamIsNullAndParentFolderIsNull(user);
        }
        List<FileSearchResultDto> results = new ArrayList<>();
        for (Folder root : rootFolders) {
            findFilesRecursive(root, query, "/" + root.getName(), results);
        }
        return results;
    }

    private void findFilesRecursive(Folder currentFolder, String query, String currentPath, List<FileSearchResultDto> results) {
        fileRepository.findByFolderAndOriginalNameContainingIgnoreCaseAndIsDeletedFalse(currentFolder, query)
                .stream()
                .map(file -> new FileSearchResultDto(file, currentPath))
                .forEach(results::add);
        for (Folder subFolder : currentFolder.getSubFolders()) {
            findFilesRecursive(subFolder, query, currentPath + "/" + subFolder.getName(), results);
        }
    }

    private Folder findParentFolder(Long folderId, Long teamId) {
        if (teamId != null) {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
            if (folderId != null) {
                Folder parentFolder = folderRepository.findById(folderId)
                        .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다."));
                if (parentFolder.getTeam() == null || !parentFolder.getTeam().getTeamId().equals(teamId)) {
                    throw new SecurityException("선택한 폴더가 해당 팀에 속해있지 않습니다.");
                }
                return parentFolder;
            } else {
                return folderRepository.findByTeamAndParentFolderIsNull(team).stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("팀의 루트 폴더를 찾을 수 없습니다."));
            }
        } else if (folderId != null) {
            Folder parentFolder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다."));
            if (parentFolder.getTeam() != null) {
                throw new SecurityException("개인 스토리지의 폴더가 아닙니다.");
            }
            return parentFolder;
        } else {
            throw new IllegalArgumentException("업로드할 위치(폴더 또는 팀)가 지정되지 않았습니다.");
        }
    }

    private FileVersion getActiveVersion(FileEntity file) {
        return file.getActiveVersion() != null ? file.getActiveVersion() :
                fileVersionRepository.findTopByFileOrderByVersionIdAsc(file)
                        .orElseThrow(() -> new RuntimeException("다운로드할 수 있는 파일 버전이 없습니다."));
    }

    private void checkFolderPermission(Folder folder, User user) {
        boolean hasPermission = false;
        // 개인 폴더인 경우: 폴더의 소유자와 요청한 사용자가 동일한지 확인
        if (folder.getTeam() == null) {
            if (folder.getOwner().getUserId().equals(user.getUserId())) {
                hasPermission = true;
            }
        }
        // 팀 폴더인 경우: 사용자가 팀 멤버이고 '편집 권한'이 있는지 확인
        else {
            hasPermission = folder.getTeam().getTeamMembers().stream()
                    .anyMatch(member -> member.getUser().getUserId().equals(user.getUserId()) && member.isCanEdit());
        }

        if (!hasPermission) {
            throw new SecurityException("폴더에 접근하거나 파일을 업로드할 권한이 없습니다.");
        }
    }

    private void checkFileEditPermission(FileEntity file, User user) {
        boolean hasPermission = file.getOwner().getUserId().equals(user.getUserId());
        if (!hasPermission && file.getFolder().getTeam() != null) {
            hasPermission = file.getFolder().getTeam().getTeamMembers().stream()
                    .anyMatch(m -> m.getUser().getUserId().equals(user.getUserId()) && m.isCanEdit());
        }
        if (!hasPermission) {
            throw new SecurityException("파일 수정 권한이 없습니다.");
        }
    }

    private void checkFileDeletePermission(FileEntity file, User user) {
        boolean hasPermission = file.getOwner().getUserId().equals(user.getUserId());
        if (!hasPermission && file.getFolder().getTeam() != null) {
            hasPermission = file.getFolder().getTeam().getTeamMembers().stream()
                    .anyMatch(m -> m.getUser().getUserId().equals(user.getUserId()) && m.isCanDelete());
        }
        if (!hasPermission) {
            throw new SecurityException("파일 삭제 권한이 없습니다.");
        }
    }

    private void checkPermanentDeletePermission(FileEntity file, User user) {
        boolean hasPermission = file.getOwner().getUserId().equals(user.getUserId());
        if (!hasPermission && file.getFolder().getTeam() != null) {
            hasPermission = file.getFolder().getTeam().getOwner().getUserId().equals(user.getUserId()) ||
                    file.getFolder().getTeam().getTeamMembers().stream()
                            .anyMatch(m -> m.getUser().getUserId().equals(user.getUserId()) && m.isCanDelete());
        }
        if (!hasPermission) {
            throw new SecurityException("파일 영구 삭제 권한이 없습니다.");
        }
    }

    private void checkFileReadPermission(FileEntity file, User user) {
        boolean hasPermission = false;
        // 개인 파일: 소유자만 읽기 가능
        if (file.getFolder().getTeam() == null) {
            if (file.getOwner().getUserId().equals(user.getUserId())) {
                hasPermission = true;
            }
        }
        // 팀 파일: 팀 멤버라면 누구나 읽기 가능
        else {
            hasPermission = file.getFolder().getTeam().getTeamMembers().stream()
                    .anyMatch(member -> member.getUser().getUserId().equals(user.getUserId()));
        }

        if (!hasPermission) {
            throw new SecurityException("파일을 읽을 권한이 없습니다.");
        }
    }

    public String generateReadOnlySasUrl(String blobName, Duration ttl) {
        // blobContainerClient는 FileService에서 이미 주입/사용 중인 필드
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        // 권한: 읽기 전용
        BlobSasPermission perm = new BlobSasPermission().setReadPermission(true);

        // 시계 오차 버퍼 1분, TTL은 파라미터로
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        OffsetDateTime expiry = OffsetDateTime.now(ZoneOffset.UTC).plus(ttl);

        BlobServiceSasSignatureValues values =
                new BlobServiceSasSignatureValues(expiry, perm).setStartTime(start);

        String sasToken = blobClient.generateSas(values);

        String containerUrl = blobContainerClient.getBlobContainerUrl(); // e.g. https://acct.blob.core.windows.net/smartcollab-files
        return containerUrl + "/" + blobName + "?" + sasToken;
    }
}