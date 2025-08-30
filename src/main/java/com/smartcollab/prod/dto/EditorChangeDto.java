package com.smartcollab.prod.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditorChangeDto {
    private Long teamId;
    private String content;
    private String editor;
}
