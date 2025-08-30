package com.smartcollab.prod.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CopyRequestDto {
    private List<String> itemUniqueKeys;
    private Long destinationFolderId;
}