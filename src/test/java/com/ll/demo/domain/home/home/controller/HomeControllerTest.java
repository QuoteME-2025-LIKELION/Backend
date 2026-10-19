package com.ll.demo.domain.home.home.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HomeController controller = new HomeController();
        ReflectionTestUtils.setField(controller, "siteName", "QUOTEME TEST");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("회귀: 메인 페이지는 그대로 응답한다")
    void showMain_stillWorks() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("QUOTEME TEST")));
    }

    @Test
    @DisplayName("보안: /secretKey 엔드포인트는 더 이상 존재하지 않는다")
    void secretKeyEndpoint_isRemoved() throws Exception {
        mockMvc.perform(get("/secretKey"))
                .andExpect(status().isNotFound());
    }
}