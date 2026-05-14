package io.mero.app.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class EmailService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${resend.api-key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    @Async
    public void sendVerificationEmail(String to, String token) {
        String link = baseUrl + "/api/auth/email/verify?token=" + token;
        String html = """
                <div style="font-family:-apple-system,sans-serif;max-width:480px;margin:0 auto;padding:32px 24px;">
                  <h2 style="font-size:20px;font-weight:700;margin-bottom:8px;">이메일 인증</h2>
                  <p style="color:#555;margin-bottom:24px;">아래 버튼을 눌러 이메일 인증을 완료해 주세요.<br>링크는 24시간 동안 유효합니다.</p>
                  <a href="%s" style="display:inline-block;background:#1a1a1a;color:#fff;text-decoration:none;padding:12px 24px;border-radius:8px;font-weight:600;">이메일 인증하기</a>
                  <p style="color:#aaa;font-size:12px;margin-top:32px;">본 메일은 발신 전용입니다.</p>
                </div>
                """.formatted(link);

        send(to, "[Mero] 이메일 인증을 완료해 주세요", html);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String html = """
                <div style="font-family:-apple-system,sans-serif;max-width:480px;margin:0 auto;padding:32px 24px;">
                  <h2 style="font-size:20px;font-weight:700;margin-bottom:8px;">비밀번호 재설정</h2>
                  <p style="color:#555;margin-bottom:16px;">아래 코드를 앱에 입력하여 비밀번호를 재설정하세요.<br>코드는 1시간 동안 유효합니다.</p>
                  <div style="background:#f5f5f5;border-radius:8px;padding:16px 24px;font-size:24px;font-weight:700;letter-spacing:4px;text-align:center;">%s</div>
                  <p style="color:#aaa;font-size:12px;margin-top:32px;">본인이 요청하지 않은 경우 이 메일을 무시하세요.</p>
                </div>
                """.formatted(token);

        send(to, "[Mero] 비밀번호 재설정 코드", html);
    }

    private void send(String to, String subject, String html) {
        Map<String, Object> payload = Map.of(
                "from", fromEmail,
                "to", new String[]{to},
                "subject", subject,
                "html", html
        );

        restClient.post()
                .uri("https://api.resend.com/emails")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}