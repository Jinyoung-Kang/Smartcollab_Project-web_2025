package com.smartcollab.prod.controller;

import com.smartcollab.prod.entity.FileEntity;
import com.smartcollab.prod.repository.FileRepository;
import com.smartcollab.prod.service.FileService; // ★ 서비스만 참조
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FilePreviewController {

    private final FileRepository fileRepository;
    private final FileService fileService; // ★ Azure SDK는 서비스 내부에서만 사용

    @GetMapping("/preview-url/{fileId}")
    public ResponseEntity<String> getDocxPreviewUrl(@PathVariable Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));

        // 활성 버전이 있으면 그 경로, 없으면 원본 storedName
        String blobName = (file.getActiveVersion() != null
                && file.getActiveVersion().getStoredPath() != null
                && !file.getActiveVersion().getStoredPath().isEmpty())
                ? file.getActiveVersion().getStoredPath()
                : file.getStoredName();

        String sasUrl = fileService.generateReadOnlySasUrl(blobName, Duration.ofMinutes(10));
        return ResponseEntity.ok(sasUrl);
    }
}
