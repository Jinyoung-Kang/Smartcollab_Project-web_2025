package com.smartcollab.prod.dto;

import com.smartcollab.prod.entity.Team;
import lombok.Getter;

@Getter
public class TeamResponseDto {
    private final Long id;
    private final String name;
    private final int memberCount;
    private final String ownerUsername;

    public TeamResponseDto(Team team) {
        this.id = team.getTeamId();
        this.name = team.getName();
        this.memberCount = team.getTeamMembers().size();
        this.ownerUsername = team.getOwner().getUsername();
    }
}