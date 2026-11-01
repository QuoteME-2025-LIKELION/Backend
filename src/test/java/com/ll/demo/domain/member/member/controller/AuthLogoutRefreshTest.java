package com.ll.demo.domain.member.member.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthLogoutRefreshTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberService memberService;

    @Test
    @DisplayName("로그아웃 후 같은 refresh 토큰으로 재발급하면 401-3 (access 토큰 없이도 로그아웃 동작)")
    void logout_invalidatesRefreshToken() throws Exception {
        Member member = memberRepository.save(Member.builder()
                .email("logout@test.com").nickname("로그아웃").password("password123").build());
        String refreshToken = memberService.genRefreshToken(member);

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout").cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"));

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getRefreshToken()).isNull();

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("401-3")));
    }
}
