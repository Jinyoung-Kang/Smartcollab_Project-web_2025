package com.smartcollab.prod.controller;

import com.smartcollab.prod.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import com.smartcollab.prod.service.DeepLTranslationService;
import com.smartcollab.prod.service.FileTextReaderService;


// AI 기능(요약, 번역)을 위한 Mock Controller.
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final FileTextReaderService textReader;
    private final DeepLTranslationService deepl;

    @PostMapping("/summarize/{fileId}")
    public ResponseEntity<String> summarizeFile(@PathVariable Long fileId,
                                                @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        String summary = aiService.summarize(fileId, userDetails.getUsername());
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/translate/{fileId}")
    public ResponseEntity<String> translate(@PathVariable Long fileId,
                                            @RequestParam("targetLanguage") String targetLanguage) {
        String text = textReader.loadTxtContent(fileId);
        String translated = deepl.translate(text, targetLanguage);
        return ResponseEntity.ok(translated);
    }
}