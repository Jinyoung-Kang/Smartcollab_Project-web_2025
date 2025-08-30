package com.smartcollab.prod.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.smartcollab.prod.entity.FileEntity;
import com.smartcollab.prod.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileTextReaderService {

    private final BlobContainerClient blobContainerClient;
    private final FileRepository fileRepository;

    public String loadTxtContent(Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));

        String name = file.getOriginalName() != null ? file.getOriginalName().toLowerCase() : "";
        if (!name.endsWith(".txt")) {
            throw new IllegalArgumentException("TXT 파일만 번역 대상입니다.");
        }

        String blobName;
        if (file.getActiveVersion() != null
                && file.getActiveVersion().getStoredPath() != null
                && !file.getActiveVersion().getStoredPath().isEmpty()) {
            blobName = file.getActiveVersion().getStoredPath();
        } else {
            blobName = file.getStoredName();
        }

        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        // SDK v12: downloadContent() -> BinaryData -> toString()
        return blobClient.downloadContent().toString();
    }
}
