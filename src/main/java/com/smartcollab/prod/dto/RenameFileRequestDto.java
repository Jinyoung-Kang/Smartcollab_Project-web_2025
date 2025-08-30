package com.smartcollab.prod.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RenameFileRequestDto {
    @NotBlank(message = "New file name is required")
    private String newName;
}
