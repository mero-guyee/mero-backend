package io.mero.app.global.config;

import io.mero.app.global.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증 필요 (permitAll 보다 먼저 매칭되어야 함)
                        .requestMatchers("/api/auth/logout").authenticated()
                        // 인증 불필요 (누구나 접근 가능)
                        .requestMatchers(
                                // health check
                                "/",
                                "/health",

                                // Public 엔드포인트
                                "/api/auth/**",
                                "/api/meta",
                                "/api/auth/email/verify",
                                "/api/auth/password/reset-request",
                                "/api/auth/password/reset",

                                // 로컬 테스트용 네이버 OAuth 콜백
                                "/api/social/naver/callback",

                                        // 법적 문서 (이용약관, 개인정보처리방침, 위치정보 이용약관)
                                "/terms",
                                "/privacy",
                                "/location",
                                "/terms.html",
                                "/privacy.html",
                                "/location.html",

                                // Swagger 엔드포인트 허용
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}