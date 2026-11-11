package com.ll.demo.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class OpenApiConfig {

    // 앱 인증 정책 원문 (Swagger 상단에 표시)
    static final String APP_GUIDE_PATH = "openapi/app-guide.md";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("QuoteME API").description(readAppGuide(APP_GUIDE_PATH)))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    // 파일이 없거나 읽기에 실패해도 기동은 계속
    static String readAppGuide(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.warn("Swagger 앱 가이드 파일이 없습니다: {}", path);
            return "";
        }
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Swagger 앱 가이드 파일을 읽지 못했습니다: {}", path, e);
            return "";
        }
    }
}
