package com.ll.demo.domain.profile.profile.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.member.member.type.MemberProvider;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.global.security.oauth2.OAuth2MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountLinkApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberService memberService;
    @Autowired private AuthTokenService authTokenService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder().email("link@test.com").nickname("연동").password("password123").build());
    }

    private MockHttpServletRequestBuilder asMember(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + authTokenService.genToken(member, 600));
    }

    @ParameterizedTest
    @ValueSource(strings = {"google", "kakao"})
    @DisplayName("인증 사용자의 연동 시작 → 302, 세션 속성 설정")
    void start_redirects(String provider) throws Exception {
        mvc.perform(asMember(get("/api/profile/account/link/" + provider)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/" + provider))
                .andExpect(request().sessionAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE, member.getId()));
    }

    @Test
    @DisplayName("미인증 → 기존 미인증 응답")
    void start_unauthenticated() throws Exception {
        mvc.perform(get("/api/profile/account/link/google"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"naver", "GOOGLE", "local"})
    @DisplayName("잘못된 provider → 400")
    void start_invalidProvider(String provider) throws Exception {
        mvc.perform(asMember(get("/api/profile/account/link/" + provider))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게스트 계정은 연동할 수 없다 → 403")
    void start_guest() throws Exception {
        Member guest = memberRepository.findByEmail("guest@guest.com").orElseGet(() -> memberRepository.save(
                Member.builder().email("guest@guest.com").nickname("게스트").password("password123").build()));

        mvc.perform(get("/api/profile/account/link/google")
                        .header("Authorization", "Bearer " + authTokenService.genToken(guest, 600)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("연동 흐름(mode 없음)에서는 세션 속성이 유지되고, 명시적 로그인을 시작하면 지워진다")
    void loginModeFilter_keepsLinkAttribute() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE, member.getId());

        mvc.perform(get("/oauth2/authorization/google").session(session)).andExpect(status().is3xxRedirection());
        assertThat(session.getAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE)).isEqualTo(member.getId());

        mvc.perform(get("/oauth2/authorization/google").param("mode", "login").session(session))
                .andExpect(status().is3xxRedirection());
        assertThat(session.getAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE)).isNull();
    }

    @Test
    @DisplayName("연동 현황에 두 provider가 모두 표시, 기존 provider 필드 유지")
    void accountInfo_listsLinkedAccounts() throws Exception {
        memberService.linkSocialAccount(member.getId(), MemberProvider.GOOGLE, "g-1");
        memberService.linkSocialAccount(member.getId(), MemberProvider.KAKAO, "k-1");

        mvc.perform(asMember(get("/api/profile/account")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("LOCAL"))
                .andExpect(jsonPath("$.linkedAccounts[*].provider", containsInAnyOrder("GOOGLE", "KAKAO")))
                .andExpect(jsonPath("$.linkedAccounts[0].linkedAt").exists());
    }
}
