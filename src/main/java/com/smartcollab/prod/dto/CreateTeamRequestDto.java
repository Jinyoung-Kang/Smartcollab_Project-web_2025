package com.smartcollab.prod.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeamRequestDto {
    @NotBlank
    private String teamName;
}
