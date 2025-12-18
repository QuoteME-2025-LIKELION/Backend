package com.ll.demo.global.config; // 패키지 경로는 본인 프로젝트에 맞게!

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해
                .allowedOrigins(
                        "http://localhost:5173",  // 로컬 프론트엔드 개발 환경
                        "https://quoteme.shop",
                        "http://localhost:8080",
                        "https://quote--me.vercel.app",   // Vercel 배포 주소
                        "https://www.quoteme.site",
                        "https://www.quoteme.site/"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true); // 쿠키/인증 정보 포함 허용 (JWT 쓸 때 필수)
    }
}