package com.ll.demo.domain.member.member.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.security.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberProfileAccessTest {

    @Autowired private MockMvc mvc;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping handlerMapping;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AuthTokenService authTokenService;

    private Member me;
    private Member other;

    @BeforeEach
    void setUp() {
        me = memberRepository.save(Member.builder().email("me@test.com").nickname("나").password("password123").build());
        other = memberRepository.save(Member.builder().email("other@test.com").nickname("남").password("password123").build());
    }

    private String bearer(Member member) {
        return "Bearer " + authTokenService.genToken(member, 600);
    }

    @Test
    @DisplayName("다른 회원 프로필 조회 → 200, email 없음")
    void otherProfile_200() throws Exception {
        mvc.perform(get("/api/profile/" + other.getId()).header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(other.getId()))
                .andExpect(jsonPath("$.nickname").value("남"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/profile/{id} 가 핸들러 매핑에 실제로 등록되어 있음")
    void profileById_isRegistered() {
        boolean registered = handlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(info -> info.getPatternValues().contains("/api/profile/{id}")
                        && info.getMethodsCondition().getMethods().contains(RequestMethod.GET));

        assertThat(registered).isTrue();
    }

    @Test
    @DisplayName("없는 회원 프로필 조회 → 404")
    void missingProfile_404() throws Exception {
        mvc.perform(get("/api/profile/999999").header("Authorization", bearer(me)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/api/profile/account 는 기존 계정 조회 그대로")
    void accountPath_unchanged() throws Exception {
        mvc.perform(get("/api/profile/account").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@test.com"));
    }

    @Test
    @DisplayName("탈퇴 회원의 유효한 토큰 → 500이 아닌 미인증 403-1")
    void deletedMemberToken_403() throws Exception {
        String token = bearer(other);
        memberRepository.delete(other);
        memberRepository.flush();

        mvc.perform(get("/api/profile/account").header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }
}
