package com.ll.demo.global.security.oauth2;

import com.ll.demo.AppConfig;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.standard.rq.Rq;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OAuth2SuccessHandlerTest {

    private static final String REDIRECT_URI = "http://front.test/oauth2/callback";

    @Autowired private MockMvc mvc;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

    private final Rq rq = mock(Rq.class);
    private OAuth2SuccessHandler handler;
    private Member member;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(authTokenService, memberService, rq);
        ReflectionTestUtils.setField(handler, "redirectUri", REDIRECT_URI);
        member = memberRepository.save(Member.builder().email("social@test.com").nickname("소셜").password("password123").build());
    }

    private String succeed(boolean newUser) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        CustomOAuth2User principal = new CustomOAuth2User(member, Map.of(), newUser);
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, new TestingAuthenticationToken(principal, null));
        return response.getRedirectedUrl();
    }

    private String refreshCookieValue() {
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(rq).setCookie(eq("refreshToken"), value.capture(), eq(60 * 60 * 24 * 30));
        return value.getValue();
    }

    @Test
    @DisplayName("URL에 refresh 토큰은 없고 access 토큰은 과도기 동안 유지")
    void url_keepsAccessTokenOnly() throws Exception {
        String url = succeed(false);

        assertThat(url).startsWith(REDIRECT_URI + "?accessToken=");
        assertThat(url).doesNotContain("refreshToken").doesNotContain(refreshCookieValue());
    }

    @Test
    @DisplayName("access·refresh 쿠키를 모두 설정하고 access 만료는 AppConfig 기준")
    void setsBothCookies() throws Exception {
        succeed(false);

        verify(rq).setCookie(eq("accessToken"), anyString(), eq(AppConfig.getAccessTokenExpirationSec()));
        assertThat(refreshCookieValue()).isNotBlank();
    }

    @Test
    @DisplayName("신규 회원만 isNewUser=true")
    void isNewUser() throws Exception {
        assertThat(succeed(true)).endsWith("&isNewUser=true");
        assertThat(succeed(false)).doesNotContain("isNewUser");
    }

    @Test
    @DisplayName("연동 모드는 토큰 쿠키를 발급하지 않고 linked로 리다이렉트")
    void linkMode_redirectsWithoutTokens() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(OAuth2MemberService.LINKED_PROVIDER_ATTRIBUTE, "kakao");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String refreshTokenBefore = member.getRefreshToken();

        handler.onAuthenticationSuccess(request, response,
                new TestingAuthenticationToken(new CustomOAuth2User(member, Map.of(), false), null));

        assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URI + "?linked=kakao");
        verifyNoInteractions(rq);
        assertThat(member.getRefreshToken()).isEqualTo(refreshTokenBefore);
    }

    @Test
    @DisplayName("발급된 refresh 쿠키로 /api/auth/refresh 성공")
    void issuedRefreshToken_works() throws Exception {
        succeed(false);

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", refreshCookieValue())))
                .andExpect(status().isOk());
    }
}
