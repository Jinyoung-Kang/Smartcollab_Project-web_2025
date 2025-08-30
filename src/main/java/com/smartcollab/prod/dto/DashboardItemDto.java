package com.smartcollab.prod.dto;

import com.smartcollab.prod.entity.FileEntity;
import com.smartcollab.prod.entity.Folder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class DashboardItemDto {
    private final Long id;
    private final String name;
    private final String type;
    private final LocalDateTime modifiedAt;
    private final String ownerName;
    private final Long size;
    private final String uniqueKey;

    public DashboardItemDto(Folder folder) {
        this.id = folder.getFolderId();
        this.name = folder.getName();
        this.type = "folder";
        this.modifiedAt = folder.getCreatedAt();
        this.ownerName = folder.getOwner().getName();
        this.size = null;
        this.uniqueKey = "folder-" + folder.getFolderId();
    }

    public DashboardItemDto(FileEntity file) {
        this.id = file.getFileId();
        this.name = file.getOriginalName();
        this.type = file.getOriginalName().contains(".") ? file.getOriginalName().substring(file.getOriginalName().lastIndexOf(".") + 1) : "file";
        this.modifiedAt = file.getCreatedAt();
        this.ownerName = file.getOwner().getName();
        this.size = file.getSize();
        this.uniqueKey = "file-" + file.getFileId();
    }
}