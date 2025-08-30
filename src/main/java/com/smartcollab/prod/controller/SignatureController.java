package com.smartcollab.prod.controller;

import com.smartcollab.prod.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;


    @PostMapping("/sign/file/{fileId}")
    public ResponseEntity<String> signLatestVersion(@PathVariable Long fileId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        try {
            signatureService.signLatestVersionOfFile(fileId, userDetails.getUsername());
            return ResponseEntity.ok("파일의 최신 버전에 성공적으로 서명했습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
