package com.ll.demo.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    @Autowired private MockMvc mvc;

    @Test
    @DisplayName("/v3/api-docs: 200, 상단 가이드(X-Client-Type) 포함, 앱 인증 API 문서화")
    void apiDocs_includeAppGuide() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("QuoteME API"))
                .andExpect(jsonPath("$.info.description").value(containsString("X-Client-Type")))
                .andExpect(jsonPath("$.paths['/api/auth/oauth2/token'].post.tags").value(hasItem("앱 인증")))
                .andExpect(jsonPath("$.paths['/api/profile/account/link/confirm'].post").exists())
                .andExpect(jsonPath("$.paths['/api/profile/setup'].put.tags").value(hasItem("앱 인증")))
                .andExpect(jsonPath("$.paths['/api/auth/refresh'].post.parameters[?(@.name == 'X-Client-Type')].in")
                        .value(hasItem("header")));
    }

    @Test
    @DisplayName("가이드 파일이 없으면 빈 설명으로 계속 (기동 실패 없음)")
    void missingGuide_returnsEmpty() {
        assertThat(OpenApiConfig.readAppGuide("openapi/does-not-exist.md")).isEmpty();
        assertThat(OpenApiConfig.readAppGuide(OpenApiConfig.APP_GUIDE_PATH)).contains("X-Client-Type");
    }
}
