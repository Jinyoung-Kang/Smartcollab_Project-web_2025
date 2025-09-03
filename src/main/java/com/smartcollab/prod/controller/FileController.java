package com.smartcollab.prod.controller;

import com.smartcollab.prod.dto.CopyRequestDto;
import com.smartcollab.prod.dto.FileResponseDto;
import com.smartcollab.prod.dto.FileSearchResultDto;
import com.smartcollab.prod.dto.MoveRequestDto;
import com.smartcollab.prod.dto.RenameFileRequestDto;
import com.smartcollab.prod.dto.VersionHistoryDto;
import com.smartcollab.prod.entity.FileEntity;
import com.smartcollab.prod.repository.FileRepository;
import com.smartcollab.prod.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor

public class FileController {
    private final FileService fileService;
    private final FileRepository fileRepository;

    @PostMapping("/upload")
    public ResponseEntity<FileResponseDto> uploadFile(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) Long folderId,
                                                      @RequestParam(required = false) Long teamId,
                                                      @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        FileEntity savedFile = fileService.uploadFile(file, userDetails.getUsername(), folderId, teamId);
        return ResponseEntity.ok(new FileResponseDto(savedFile));
    }

    @GetMapping("/download-by-id/{fileId}")
    public ResponseEntity<Resource> downloadFileById(@PathVariable Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));
        Resource resource = fileService.downloadFile(fileId);
        String originalName = file.getOriginalName();
        String encodedOriginalName = URLEncoder.encode(originalName, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedOriginalName + "\"")
                .body(resource);
    }

    // 파일 확장자를 기반으로 Content-Type을 직접 결정하는 방식
    @GetMapping("/view/{fileId}")
    public ResponseEntity<Resource> viewFile(@PathVariable Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));

        Resource resource = fileService.downloadFile(fileId);

        String originalName = file.getOriginalName().toLowerCase();
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // 기본값

        if (originalName.endsWith(".pdf")) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
        } else if (originalName.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (originalName.endsWith(".jpg") || originalName.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else if (originalName.endsWith(".gif")) {
            contentType = MediaType.IMAGE_GIF_VALUE;
        } else if (originalName.endsWith(".txt") || originalName.endsWith(".md")) {
            contentType = MediaType.TEXT_PLAIN_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getOriginalName() + "\"")
                .body(resource);
    }

    @PutMapping("/{fileId}/rename")
    public ResponseEntity<String> renameFile(@PathVariable Long fileId,
                                             @Valid @RequestBody RenameFileRequestDto requestDto,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        fileService.renameFile(fileId, requestDto.getNewName(), userDetails.getUsername());
        return ResponseEntity.ok("파일 이름이 변경되었습니다.");
    }

    @PostMapping("/move")
    public ResponseEntity<String> moveItems(@RequestBody MoveRequestDto requestDto, @AuthenticationPrincipal UserDetails userDetails) {
        fileService.moveItems(requestDto.getItemUniqueKeys(), requestDto.getDestinationFolderId(), userDetails.getUsername());
        return ResponseEntity.ok("선택한 항목을 이동했습니다.");
    }

    @PostMapping("/copy")
    public ResponseEntity<String> copyItems(@RequestBody CopyRequestDto requestDto, @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        fileService.copyFiles(requestDto.getItemUniqueKeys(), requestDto.getDestinationFolderId(), userDetails.getUsername());
        return ResponseEntity.ok("선택한 파일을 복사했습니다.");
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<String> moveFileToTrash(@PathVariable Long fileId,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        fileService.moveFileToTrash(fileId, userDetails.getUsername());
        return ResponseEntity.ok("파일이 휴지통으로 이동되었습니다.");
    }

    @GetMapping("/trash")
    public ResponseEntity<List<FileResponseDto>> getFilesInTrash(@AuthenticationPrincipal UserDetails userDetails) {
        List<FileResponseDto> filesInTrash = fileService.getFilesInTrash(userDetails.getUsername());
        return ResponseEntity.ok(filesInTrash);
    }

    @PostMapping("/trash/{fileId}/restore")
    public ResponseEntity<String> restoreFileFromTrash(@PathVariable Long fileId,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        fileService.restoreFileFromTrash(fileId, userDetails.getUsername());
        return ResponseEntity.ok("파일이 복원되었습니다.");
    }

    @DeleteMapping("/trash/{fileId}/permanent")
    public ResponseEntity<String> deleteFilePermanently(@PathVariable Long fileId,
                                                        @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        fileService.deleteFilePermanently(fileId, userDetails.getUsername());
        return ResponseEntity.ok("파일이 영구적으로 삭제되었습니다.");
    }

    @PostMapping("/save/{fileId}")
    public ResponseEntity<String> saveVersion(@PathVariable Long fileId,
                                              @RequestBody Map<String, String> payload,
                                              @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        String content = payload.get("content");
        fileService.saveNewVersion(fileId, content, userDetails.getUsername());
        return ResponseEntity.ok("새 버전이 저장되었습니다.");
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<String> getLatestFileContent(@PathVariable Long fileId) throws IOException {
        String content = fileService.getLatestFileContent(fileId);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/{fileId}/history")
    public ResponseEntity<List<VersionHistoryDto>> getVersionHistory(@PathVariable Long fileId) {
        List<VersionHistoryDto> history = fileService.getVersionHistory(fileId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{fileId}/restore-version/{versionId}")
    public ResponseEntity<String> restoreVersion(@PathVariable Long fileId,
                                                 @PathVariable Long versionId,
                                                 @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        fileService.restoreVersion(fileId, versionId, userDetails.getUsername());
        return ResponseEntity.ok("파일이 선택한 버전으로 복원되었습니다.");
    }

    @GetMapping("/search")
    public ResponseEntity<List<FileSearchResultDto>> searchFiles(
            @RequestParam String query,
            @RequestParam(required = false) Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<FileSearchResultDto> results = fileService.searchFiles(query, teamId, userDetails.getUsername());
        return ResponseEntity.ok(results);
    }

}
