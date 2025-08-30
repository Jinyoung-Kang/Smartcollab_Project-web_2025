package com.smartcollab.prod.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobConfig {

    @Bean
    public BlobContainerClient blobContainerClient(
            BlobServiceClient serviceClient,
            @Value("${spring.cloud.azure.storage.blob.container-name}") String containerName) {

        BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
        client.createIfNotExists(); // 컨테이너가 없으면 자동 생성
        return client;
    }
}
