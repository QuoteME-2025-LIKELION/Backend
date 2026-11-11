package com.ll.demo.domain.member.member.controller;

import com.jayway.jsonpath.JsonPath;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppClientAuthTest {

    private static final String CLIENT_HEADER = "X-Client-Type";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberService memberService;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .email("app@test.com").nickname("앱").password(passwordEncoder.encode("password123")).build());
    }

    private MockHttpServletRequestBuilder app(MockHttpServletRequestBuilder builder) {
        return builder.header(CLIENT_HEADER, "app");
    }

    private MockHttpServletRequestBuilder json(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static List<String> tokenCookies(MockHttpServletResponse response) {
        return response.getHeaders("Set-Cookie").stream()
                .filter(c -> c.startsWith("accessToken=") || c.startsWith("refreshToken="))
                .toList();
    }

    private static String loginBody() {
        return "{\"email\":\"app@test.com\",\"password\":\"password123\"}";
    }

    @Test
    @DisplayName("앱 이메일 로그인: 본문에 refreshToken, 토큰 쿠키 없음, 그 refresh로 재발급 가능")
    void appLogin_returnsRefreshTokenInBody() throws Exception {
        MockHttpServletResponse response = mvc.perform(app(json("/api/auth/login", loginBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse();

        assertThat(tokenCookies(response)).isEmpty();
        String refreshToken = JsonPath.read(response.getContentAsString(), "$.data.refreshToken");
        assertThat(refreshToken).isEqualTo(memberRepository.findById(member.getId()).orElseThrow().getRefreshToken());
    }

    @Test
    @DisplayName("웹 이메일 로그인: 본문에 refreshToken 키가 없고 access·refresh 쿠키는 그대로")
    void webLogin_unchanged() throws Exception {
        MockHttpServletResponse response = mvc.perform(json("/api/auth/login", loginBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.item.email").value("app@test.com"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn().getResponse();

        assertThat(tokenCookies(response)).hasSize(2);
    }

    @Test
    @DisplayName("앱 회원가입·게스트 로그인도 본문에 refreshToken, 토큰 쿠키 없음")
    void appSignupAndGuest_returnRefreshTokenInBody() throws Exception {
        MockHttpServletResponse signup = mvc.perform(app(json("/api/auth/signup",
                        "{\"email\":\"new-app@test.com\",\"password\":\"password123\",\"birthYear\":\"2000\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse();
        assertThat(tokenCookies(signup)).isEmpty();

        MockHttpServletResponse guest = mvc.perform(app(post("/api/auth/guest-login")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse();
        assertThat(tokenCookies(guest)).isEmpty();
    }

    @Test
    @DisplayName("웹 회원가입·게스트 로그인 응답에는 refreshToken 키가 없다")
    void webSignupAndGuest_noRefreshTokenInBody() throws Exception {
        MockHttpServletResponse signup = mvc.perform(json("/api/auth/signup",
                        "{\"email\":\"new-web@test.com\",\"password\":\"password123\",\"birthYear\":\"2000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn().getResponse();
        assertThat(tokenCookies(signup)).hasSize(2);

        mvc.perform(post("/api/auth/guest-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    @DisplayName("본문 refresh로 재발급(쿠키 없음): 앱이면 Set-Cookie 없이 accessToken만")
    void refresh_withBody() throws Exception {
        String refreshToken = memberService.genRefreshToken(member);

        MockHttpServletResponse response = mvc.perform(app(json("/api/auth/refresh",
                        "{\"refreshToken\":\"" + refreshToken + "\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse();

        assertThat(tokenCookies(response)).isEmpty();
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("본문 refresh가 쿠키보다 우선")
    void refresh_bodyOverCookie() throws Exception {
        String refreshToken = memberService.genRefreshToken(member);

        mvc.perform(json("/api/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}")
                        .cookie(new Cookie("refreshToken", "stale")))
                .andExpect(status().isOk());
        mvc.perform(json("/api/auth/refresh", "{\"refreshToken\":\"stale\"}")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(startsWith("401-3")));
    }

    @Test
    @DisplayName("웹 쿠키 재발급은 기존처럼 access 쿠키를 설정")
    void webRefresh_setsAccessCookie() throws Exception {
        String refreshToken = memberService.genRefreshToken(member);

        MockHttpServletResponse response = mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        // 필터 자동 재발급과 컨트롤러가 각각 access 쿠키를 설정 (refresh 쿠키는 건드리지 않음)
        assertThat(tokenCookies(response)).isNotEmpty().allMatch(c -> c.startsWith("accessToken="));
    }

    @Test
    @DisplayName("refresh가 본문·쿠키 모두 없으면 기존처럼 401-2")
    void refresh_missing() throws Exception {
        mvc.perform(app(post("/api/auth/refresh")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(startsWith("401-2")));
    }

    @Test
    @DisplayName("만료 access + 본문 refresh로 로그아웃하면 그 refresh로 재발급 시 401-3, 앱은 Set-Cookie 없음")
    void appLogout_withExpiredAccessAndBodyRefresh() throws Exception {
        String refreshToken = memberService.genRefreshToken(member);
        String expiredAccessToken = authTokenService.genToken(member, -60);

        MockHttpServletResponse response = mvc.perform(app(json("/api/auth/logout", "{\"refreshToken\":\"" + refreshToken + "\"}"))
                        .header("Authorization", "Bearer " + expiredAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andReturn().getResponse();

        assertThat(tokenCookies(response)).isEmpty();
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getRefreshToken()).isNull();
        mvc.perform(app(json("/api/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(startsWith("401-3")));
    }

    @Test
    @DisplayName("Bearer 인증 사용자는 본문 없이 로그아웃해도 refresh가 무효화")
    void logout_withBearerOnly() throws Exception {
        memberService.genRefreshToken(member);

        mvc.perform(app(post("/api/auth/logout")).header("Authorization", "Bearer " + authTokenService.genToken(member, 600)))
                .andExpect(status().isOk());

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("Origin 헤더 없는 POST(RN 요청)는 CORS 필터를 통과")
    void postWithoutOrigin_passesCors() throws Exception {
        MockHttpServletResponse response = mvc.perform(app(json("/api/auth/login", loginBody())))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
