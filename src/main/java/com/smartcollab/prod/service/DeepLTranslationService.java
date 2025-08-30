package com.smartcollab.prod.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class DeepLTranslationService {

    @Value("${deepl.api.key:}")
    private String apiKey;

    @Value("${deepl.api.endpoint:https://api-free.deepl.com}")
    private String apiEndpoint;

    private final RestTemplate rt = new RestTemplate();

    // 간단 문자 청크(안전 상한 3500자) — 긴 파일 대비
    private List<String> chunk(String text, int maxLen) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return List.of("");
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + maxLen, text.length());
            out.add(text.substring(i, end));
            i = end;
        }
        return out;
    }

    private String normalizeTarget(String lang) {
        if (lang == null) return "EN";
        lang = lang.trim().toLowerCase();
        switch (lang) {
            case "en": return "EN"; // 필요시 EN-GB/EN-US로 분기
            case "ko": return "KO";
            default: return lang.toUpperCase();
        }
    }

    public String translate(String text, String targetLang) {
        if (apiKey == null || apiKey.isBlank()) {
            // 키 없으면 기존 MOCK 반환
            return "[MOCK] (" + targetLang + ") " + text;
        }

        String url = apiEndpoint + "/v2/translate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String target = normalizeTarget(targetLang);

        // 여러 청크를 'text=' 다중 파라미터로 전송
        List<String> chunks = chunk(text, 3500);
        StringBuilder form = new StringBuilder();
        form.append("auth_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        form.append("&target_lang=").append(URLEncoder.encode(target, StandardCharsets.UTF_8));
        // 서식 유지(줄바꿈 등)
        form.append("&preserve_formatting=1");
        for (String c : chunks) {
            form.append("&text=").append(URLEncoder.encode(c, StandardCharsets.UTF_8));
        }

        HttpEntity<String> entity = new HttpEntity<>(form.toString(), headers);
        ResponseEntity<Map> res = rt.exchange(url, HttpMethod.POST, entity, Map.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            throw new RuntimeException("DeepL 호출 실패: " + res.getStatusCode());
        }

        // 응답: { translations: [ { text: "...", detected_source_language: "..." }, ... ] }
        Map body = res.getBody();
        List<Map<String, Object>> translations = (List<Map<String, Object>>) body.get("translations");
        if (translations == null || translations.isEmpty()) {
            throw new RuntimeException("DeepL 응답이 비어 있습니다.");
        }

        StringBuilder merged = new StringBuilder();
        for (Map<String, Object> t : translations) {
            merged.append((String) t.get("text"));
        }
        return merged.toString();
    }
}
