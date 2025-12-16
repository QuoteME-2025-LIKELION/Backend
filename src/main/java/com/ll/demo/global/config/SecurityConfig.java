package com.ll.demo.global.config; // 패키지 경로는 본인 프로젝트에 맞게!

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CORS 설정 적용 (우리가 만든 설정을 따르라고 지시)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. CSRF 비활성화 (API 서버는 보통 끔)
                .csrf(csrf -> csrf.disable())

                // 3. 모든 요청 허용 (개발 단계이므로 일단 다 열어둡니다)
                // ★ 나중에 로그인 구현하면 여기서 권한 설정을 바꿔야 합니다.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll()
                );

        return http.build();
    }

    // 4. 구체적인 CORS 설정 (아까 WebMvcConfig보다 힘이 셉니다)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 프론트엔드 주소들
        configuration.addAllowedOrigin("http://localhost:5173"); // 로컬 개발용
        configuration.addAllowedOrigin("https://quoteme.shop");  // 배포된 프론트 주소

        // 허용할 메서드 (GET, POST 등 다 허용)
        configuration.addAllowedMethod("*");

        // 허용할 헤더 (Authorization 등 다 허용)
        configuration.addAllowedHeader("*");

        // 쿠키나 인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}