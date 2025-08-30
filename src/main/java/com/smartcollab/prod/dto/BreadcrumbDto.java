package com.smartcollab.prod.dto;

import com.smartcollab.prod.entity.Folder;
import lombok.Getter;

@Getter
public class BreadcrumbDto {
    private final Long id;
    private final String name;

    public BreadcrumbDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public BreadcrumbDto(Folder folder) {
        this.id = folder.getFolderId();
        this.name = folder.getName();
    }
}