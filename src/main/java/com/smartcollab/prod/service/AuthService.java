package com.smartcollab.prod.service;

import com.smartcollab.prod.dto.LoginRequestDto;
import com.smartcollab.prod.dto.LoginResponseDto;
import com.smartcollab.prod.dto.SignUpRequestDto;
import com.smartcollab.prod.entity.User;
import com.smartcollab.prod.repository.UserRepository;
import com.smartcollab.prod.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FolderService folderService; // 루트 폴더 생성을 위해 추가

    private final AuthenticationManager authenticationManager;

    @Transactional
    public void signUp(SignUpRequestDto signUpRequestDto) {
        if (!signUpRequestDto.getPassword().equals(signUpRequestDto.getPasswordCheck())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        if (userRepository.existsByUsername(signUpRequestDto.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (StringUtils.hasText(signUpRequestDto.getEmail()) && userRepository.existsByEmail(signUpRequestDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());
        User.Role role = userRepository.count() == 0 ? User.Role.ADMIN : User.Role.USER;

        User user = User.builder()
                .username(signUpRequestDto.getUsername())
                .password(encodedPassword)
                .name(signUpRequestDto.getName())
                .email(signUpRequestDto.getEmail())
                .role(role)
                .build();

        User savedUser = userRepository.save(user);


        folderService.createRootFolderForUser(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getUsername(),
                        loginRequestDto.getPassword()
                )
        );

        // 인증된 사용자 이름을 기반으로 JWT를 생성
        String token = jwtUtil.generateToken(authentication.getName());
        return new LoginResponseDto(token);
    }
}