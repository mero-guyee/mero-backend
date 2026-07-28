package io.mero.app.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * 외부 HTTP 호출용 공용 RestTemplate.
     *
     * <p>타임아웃이 없으면 상대가 응답을 붙들고 있을 때 요청 스레드가 무한정 묶인다.
     * 현재 유일한 사용처인 JWKS 조회는 응답이 작고 빨라야 정상이므로 짧게 잡는다.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(3))
                .build();
    }
}
