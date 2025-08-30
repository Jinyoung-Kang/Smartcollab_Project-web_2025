package com.smartcollab.prod.controller;

import com.smartcollab.prod.dto.CreateTeamRequestDto;
import com.smartcollab.prod.dto.InviteTeamMemberRequestDto;
import com.smartcollab.prod.dto.TeamMemberResponseDto;
import com.smartcollab.prod.dto.TeamResponseDto;
import com.smartcollab.prod.dto.UpdatePermissionRequestDto;
import com.smartcollab.prod.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;


    @PostMapping
    public ResponseEntity<String> createTeam(@Valid @RequestBody CreateTeamRequestDto requestDto,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        teamService.createTeam(requestDto, userDetails);
        return ResponseEntity.ok("팀이 성공적으로 생성되었습니다.");
    }


    @PostMapping("/{teamId}/invite")
    public ResponseEntity<String> inviteMember(@PathVariable Long teamId,
                                               @Valid @RequestBody InviteTeamMemberRequestDto requestDto,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        teamService.inviteMember(teamId, requestDto, userDetails);
        return ResponseEntity.ok("팀원에게 초대 메시지를 보냈습니다.");
    }


    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponseDto>> getTeamMembers(@PathVariable Long teamId) {
        List<TeamMemberResponseDto> members = teamService.getTeamMembers(teamId)
                .stream()
                .map(TeamMemberResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }


    @PutMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<String> updateMemberPermissions(@PathVariable Long teamId,
                                                          @PathVariable Long memberId,
                                                          @RequestBody UpdatePermissionRequestDto requestDto,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        teamService.updateMemberPermissions(teamId, memberId, requestDto, userDetails);
        return ResponseEntity.ok("팀원 권한이 수정되었습니다.");
    }


    @GetMapping("/my-teams")
    public ResponseEntity<List<TeamResponseDto>> getMyTeams(@AuthenticationPrincipal UserDetails userDetails) {
        List<TeamResponseDto> myTeams = teamService.getMyTeams(userDetails.getUsername())
                .stream()
                .map(TeamResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(myTeams);
    }


    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<String> removeMember(@PathVariable Long teamId,
                                               @PathVariable Long memberId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        teamService.removeMember(teamId, memberId, userDetails);
        return ResponseEntity.ok("팀원이 추방되었습니다.");
    }


    @PostMapping("/{teamId}/leave")
    public ResponseEntity<String> leaveTeam(@PathVariable Long teamId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        teamService.leaveTeam(teamId, userDetails);
        return ResponseEntity.ok("팀에서 탈퇴했습니다.");
    }

    @PostMapping("/{teamId}/delegate/{memberId}")
    public ResponseEntity<String> delegateLeadership(@PathVariable Long teamId,
                                                     @PathVariable Long memberId,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        teamService.delegateLeadership(teamId, memberId, userDetails);
        return ResponseEntity.ok("팀장이 위임되었습니다.");
    }


    @DeleteMapping("/{teamId}")
    public ResponseEntity<String> deleteTeam(@PathVariable Long teamId,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        teamService.deleteTeam(teamId, userDetails);
        return ResponseEntity.ok("팀이 성공적으로 삭제되었습니다.");
    }
}