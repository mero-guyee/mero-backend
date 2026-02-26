package io.mero.app.domain.social.controller;

import io.mero.app.domain.social.dto.NaverAuthUrlResponse;
import io.mero.app.domain.social.dto.NaverConnectRequest;
import io.mero.app.domain.social.dto.NaverConnectResponse;
import io.mero.app.domain.social.dto.NaverStatusResponse;
import io.mero.app.domain.social.service.NaverOAuthService;
import io.mero.app.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Social - Naver", description = "네이버 소셜 연동 API")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/social/naver")
@RequiredArgsConstructor
public class NaverOAuthController {

    private final NaverOAuthService naverOAuthService;

    @Operation(summary = "네이버 인증 URL 조회", description = "네이버 OAuth 인증 URL과 state를 반환합니다")
    @GetMapping("/auth-url")
    public ResponseEntity<NaverAuthUrlResponse> getAuthUrl() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(naverOAuthService.getAuthUrl(userId));
    }

    @Operation(summary = "네이버 연동 완료", description = "Authorization code를 교환하여 네이버 계정을 연동합니다")
    @PostMapping("/connect")
    public ResponseEntity<NaverConnectResponse> connect(
            @Valid @RequestBody NaverConnectRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(naverOAuthService.connect(userId, request));
    }

    @Operation(summary = "네이버 연동 상태 조회", description = "현재 네이버 연동 상태를 조회합니다")
    @GetMapping("/status")
    public ResponseEntity<NaverStatusResponse> getStatus() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(naverOAuthService.getStatus(userId));
    }

    @Operation(summary = "네이버 연동 해제", description = "네이버 소셜 연동을 해제합니다")
    @DeleteMapping
    public ResponseEntity<Void> disconnect() {
        Long userId = SecurityUtil.getCurrentUserId();
        naverOAuthService.disconnect(userId);
        return ResponseEntity.noContent().build();
    }

    @Hidden
    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback(
            @RequestParam String code,
            @RequestParam String state) {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"><title>Naver OAuth Callback</title></head>
                <body style="font-family:monospace; padding:40px;">
                  <h2>✅ 네이버 인증 성공</h2>
                  <p>아래 값을 복사해서 Swagger의 <code>POST /api/social/naver/connect</code>에 입력하세요.</p>
                  <table border="1" cellpadding="10" style="border-collapse:collapse;">
                    <tr><th>code</th><td id="code">%s</td></tr>
                    <tr><th>state</th><td id="state">%s</td></tr>
                  </table>
                  <br>
                  <details>
                    <summary>Request Body (클릭해서 복사)</summary>
                    <pre style="background:#f4f4f4;padding:16px;">
                {
                  "code": "%s",
                  "state": "%s"
                }
                    </pre>
                  </details>
                </body>
                </html>
                """.formatted(code, state, code, state);

        return ResponseEntity.ok(html);
    }
}
