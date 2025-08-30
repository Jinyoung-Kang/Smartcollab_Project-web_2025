package com.smartcollab.prod.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePermissionRequestDto {
    private boolean canEdit;
    private boolean canDelete;
    private boolean canInvite;
}
