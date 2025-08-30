package com.smartcollab.prod.service;

import com.smartcollab.prod.dto.CreateFolderRequestDto;
import com.smartcollab.prod.dto.FolderTreeDto;
import com.smartcollab.prod.entity.FileEntity;
import com.smartcollab.prod.entity.Folder;
import com.smartcollab.prod.entity.Team;
import com.smartcollab.prod.entity.User;
import com.smartcollab.prod.repository.FileRepository;
import com.smartcollab.prod.repository.FolderRepository;
import com.smartcollab.prod.repository.TeamRepository;
import com.smartcollab.prod.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final FileRepository fileRepository;
    @Lazy
    private final FileService fileService;

    @Transactional
    public void createRootFolderForUser(User user) {
        Folder rootFolder = Folder.builder()
                .name(user.getUsername() + "의 루트 폴더")
                .owner(user)
                .team(null)
                .parentFolder(null)
                .build();
        folderRepository.save(rootFolder);
    }

    @Transactional
    public void createFolder(CreateFolderRequestDto requestDto, UserDetails currentUserDetails) {
        User owner = userRepository.findByUsername(currentUserDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team team = null;
        Folder parentFolder = null;

        if (requestDto.getTeamId() != null) {
            team = teamRepository.findById(requestDto.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
            if (requestDto.getParentFolderId() != null) {
                parentFolder = folderRepository.findById(requestDto.getParentFolderId())
                        .orElseThrow(() -> new IllegalArgumentException("상위 폴더를 찾을 수 없습니다."));
                if (parentFolder.getTeam() == null || !parentFolder.getTeam().getTeamId().equals(requestDto.getTeamId())) {
                    throw new SecurityException("선택한 폴더가 해당 팀에 속해있지 않습니다.");
                }
            } else {
                parentFolder = folderRepository.findByTeamAndParentFolderIsNull(team).stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("팀의 루트 폴더를 찾을 수 없습니다."));
            }
        } else if (requestDto.getParentFolderId() != null) {
            parentFolder = folderRepository.findById(requestDto.getParentFolderId())
                    .orElseThrow(() -> new IllegalArgumentException("상위 폴더를 찾을 수 없습니다."));
            if (parentFolder.getTeam() != null) {
                throw new SecurityException("개인 스토리지의 폴더가 아닙니다.");
            }
        } else {
            throw new IllegalArgumentException("상위 폴더 또는 팀이 지정되지 않았습니다.");
        }

        checkFolderPermission(parentFolder, owner);

        Folder newFolder = Folder.builder()
                .name(requestDto.getFolderName())
                .owner(owner)
                .team(team)
                .parentFolder(parentFolder)
                .build();
        folderRepository.save(newFolder);
    }

    @Transactional(readOnly = true)
    public List<FolderTreeDto> getFolderTreeForUser(User user) {
        List<Folder> rootFolders = folderRepository.findByOwnerAndTeamIsNullAndParentFolderIsNull(user);
        return rootFolders.stream()
                .map(FolderTreeDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderTreeDto> getFolderTreeForTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        List<Folder> rootFolders = folderRepository.findByTeamAndParentFolderIsNull(team);
        return rootFolders.stream()
                .map(FolderTreeDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFolderPermanently(Long folderId, User user) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 폴더를 찾을 수 없습니다."));
        checkFolderDeletePermission(folder, user);

        List<Folder> subFolders = folderRepository.findByParentFolder(folder);
        for (Folder subFolder : subFolders) {
            deleteFolderPermanently(subFolder.getFolderId(), user);
        }

        List<FileEntity> filesInFolder = fileRepository.findByFolderAndIsDeletedFalse(folder);
        for (FileEntity file : filesInFolder) {
            try {
                fileService.deleteFilePermanently(file.getFileId(), user.getUsername());
            } catch (IOException e) {
                throw new RuntimeException("폴더 내 파일 삭제 중 오류 발생: " + file.getOriginalName(), e);
            }
        }
        folderRepository.delete(folder);
    }

    @Transactional
    public void renameFolder(Long folderId, String newName, User user) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다."));
        checkFolderPermission(folder, user); // 이름 변경은 편집 권한으로 확인
        folder.changeName(newName);
        folderRepository.save(folder);
    }

    private void checkFolderPermission(Folder folder, User user) {
        boolean hasPermission = false;
        // 개인 폴더인 경우, 소유자만 접근 가능
        if (folder.getOwner().getUserId().equals(user.getUserId())) {
            hasPermission = true;
        }
        // 팀 폴더인 경우, '편집 권한'이 있는 멤버만 접근 가능
        else if (folder.getTeam() != null) {
            hasPermission = folder.getTeam().getTeamMembers().stream()
                    .anyMatch(member -> member.getUser().getUserId().equals(user.getUserId()) && member.isCanEdit());
        }
        if (!hasPermission) {
            throw new SecurityException("폴더에 생성 또는 편집 권한이 없습니다.");
        }
    }

    private void checkFolderDeletePermission(Folder folder, User user) {
        boolean hasPermission = false;
        // 개인 폴더는 소유자만 삭제 가능
        if (folder.getTeam() == null) {
            if (folder.getOwner().getUserId().equals(user.getUserId())) {
                hasPermission = true;
            }
        }
        // 팀 폴더는 팀장이거나 '삭제 권한'이 있는 멤버만 삭제 가능
        else {
            hasPermission = folder.getTeam().getTeamMembers().stream()
                    .anyMatch(member -> member.getUser().getUserId().equals(user.getUserId()) && (member.isTeamLeader() || member.isCanDelete()));
        }
        if (!hasPermission) {
            throw new SecurityException("폴더를 삭제할 권한이 없습니다.");
        }
    }
}
