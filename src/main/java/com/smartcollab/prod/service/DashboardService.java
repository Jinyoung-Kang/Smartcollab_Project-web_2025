package com.smartcollab.prod.service;

import com.smartcollab.prod.dto.DashboardItemDto;
import com.smartcollab.prod.dto.BreadcrumbDto;
import com.smartcollab.prod.dto.FolderContentDto;
import com.smartcollab.prod.entity.Folder;
import com.smartcollab.prod.entity.Team;
import com.smartcollab.prod.entity.User;
import com.smartcollab.prod.repository.FileRepository;
import com.smartcollab.prod.repository.FolderRepository;
import com.smartcollab.prod.repository.TeamRepository;
import com.smartcollab.prod.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<DashboardItemDto> getRootDashboardItems(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        List<Folder> rootFolders = folderRepository.findByOwnerAndTeamIsNullAndParentFolderIsNull(user);
        return rootFolders.stream().map(DashboardItemDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FolderContentDto getFolderContents(Long folderId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다."));

        checkFolderPermission(folder, user);

        List<DashboardItemDto> subFolders = folderRepository.findByParentFolder(folder)
                .stream()
                .map(DashboardItemDto::new)
                .collect(Collectors.toList());
        List<DashboardItemDto> files = fileRepository.findByFolderAndIsDeletedFalse(folder)
                .stream()
                .map(DashboardItemDto::new)
                .collect(Collectors.toList());

        List<DashboardItemDto> items = Stream.concat(subFolders.stream(), files.stream()).collect(Collectors.toList());

        List<BreadcrumbDto> path = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            if (current.getParentFolder() != null || current.getTeam() == null) {
                path.add(new BreadcrumbDto(current));
            }
            current = current.getParentFolder();
        }

        if (folder.getTeam() != null) {
            path.add(new BreadcrumbDto(folder.getTeam().getTeamId(), folder.getTeam().getName()));
        }

        Collections.reverse(path);

        return new FolderContentDto(items, path);
    }

    @Transactional(readOnly = true)
    public FolderContentDto getTeamRootItems(Long teamId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        boolean isMember = team.getTeamMembers().stream().anyMatch(member -> member.getUser().getUserId().equals(user.getUserId()));
        if (!isMember) {
            throw new SecurityException("팀에 접근할 권한이 없습니다.");
        }

        Optional<Folder> rootFolderOpt = folderRepository.findByTeamAndParentFolderIsNull(team).stream().findFirst();

        if (rootFolderOpt.isPresent()) {
            Folder rootFolder = rootFolderOpt.get();
            return getFolderContents(rootFolder.getFolderId(), username);
        } else {
            List<BreadcrumbDto> path = Collections.singletonList(new BreadcrumbDto(team.getTeamId(), team.getName()));
            return new FolderContentDto(new ArrayList<>(), path);
        }
    }

    private void checkFolderPermission(Folder folder, User user) {
        boolean hasPermission = false;
        // 개인 폴더인 경우: 폴더의 소유자와 요청한 사용자가 동일한지 확인
        if (folder.getOwner().getUserId().equals(user.getUserId())) {
            hasPermission = true;
        }
        // 팀 폴더인 경우: 사용자가 팀 멤버인지 확인 (Dashboard에서는 보기 권한만 확인)
        else if (folder.getTeam() != null) {
            hasPermission = folder.getTeam().getTeamMembers().stream()
                    .anyMatch(member -> member.getUser().getUserId().equals(user.getUserId()));
        }

        if (!hasPermission) {
            throw new SecurityException("폴더에 접근할 권한이 없습니다.");
        }
    }
}
