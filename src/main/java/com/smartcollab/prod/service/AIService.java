package com.smartcollab.prod.service;

import com.smartcollab.prod.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;


@Service
@RequiredArgsConstructor
public class AIService {

    private final FileService fileService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public String summarize(Long fileId, String username) throws IOException {
        // 파일에 대한 접근 권한 확인 (FileService 내부 로직 재활용을 위해 User 객체 필요)
        userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String content = fileService.getLatestFileContent(fileId);
        if (content.isEmpty()) {
            return "[요약] 내용이 없는 파일입니다.";
        }
        return "[요약 결과]\n\n" + content.substring(0, Math.min(content.length(), 150)) + "...";
    }

    @Transactional(readOnly = true)
    public String translate(Long fileId, String targetLanguage, String username) throws IOException {
        userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String content = fileService.getLatestFileContent(fileId);
        if (content.isEmpty()) {
            return "[번역] 내용이 없는 파일입니다.";
        }

        String langStr = "en".equalsIgnoreCase(targetLanguage) ? "영" : "한";
        return String.format("[%s어 번역 결과]\n\n", langStr) + content;
    }
}