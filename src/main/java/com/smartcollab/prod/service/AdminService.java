package com.smartcollab.prod.service;

import com.smartcollab.prod.entity.Team;
import com.smartcollab.prod.entity.User;
import com.smartcollab.prod.repository.TeamRepository;
import com.smartcollab.prod.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }
}
