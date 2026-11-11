package com.ll.demo.global.security.oauth2;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.member.member.type.MemberProvider;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.standard.rq.Rq;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppOAuth2LoginTest {

    static final String APP_URI = "quoteme://oauth2/callback";
    static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk-verifier";

    @Autowired private MockMvc mvc;
    @Autowired private AppOAuth2Service appOAuth2Service;
    @Autowired private OAuth2ExchangeCodeRepository exchangeCodeRepository;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private EntityManager em;

    private Member member;

    static String challenge(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder().email("app@test.com").nickname("앱").password("password123").build());
    }

    private ResultActions start(String redirectUri, String codeChallenge, String method, MockHttpSession session) throws Exception {
        var request = get("/oauth2/authorization/google").param("mode", "login").param("client", "app").session(session);
        if (redirectUri != null) request.param("redirect_uri", redirectUri);
        if (codeChallenge != null) request.param("code_challenge", codeChallenge);
        if (method != null) request.param("code_challenge_method", method);
        return mvc.perform(request);
    }

    private ResultActions exchange(String code, String verifier) throws Exception {
        return mvc.perform(post("/api/auth/oauth2/token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"codeVerifier\":\"" + verifier + "\"}"));
    }

    private void expectInvalidCode(ResultActions actions) throws Exception {
        actions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400"))
                .andExpect(jsonPath("$.message").value("400-30 - " + AppOAuth2Service.INVALID_CODE_MESSAGE))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    private MockHttpServletRequest appSessionRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AppOAuth2Service.startAppSession(request.getSession(), APP_URI, challenge(VERIFIER), false);
        return request;
    }

    private String succeedAsApp(Member target, boolean newUser, Rq rq) throws Exception {
        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(authTokenService, memberService, rq, appOAuth2Service);
        MockHttpServletRequest request = appSessionRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response,
                new TestingAuthenticationToken(new CustomOAuth2User(target, Map.of(), newUser), null));
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
        assertThat(request.getSession().getAttribute(AppOAuth2Service.APP_CLIENT_ATTRIBUTE)).isNull();
        return response.getRedirectedUrl();
    }

    private static String codeFrom(String url) {
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("code");
    }

    @Test
    @DisplayName("시작: 허용 URI·S256 → 302(구글), 세션에 앱 속성 저장")
    void start_allowed_redirects() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MvcResult result = start(APP_URI, challenge(VERIFIER), "S256", session)
                .andExpect(status().isFound()).andReturn();

        assertThat(result.getResponse().getRedirectedUrl()).contains("accounts.google.com");
        assertThat(session.getAttribute(AppOAuth2Service.APP_CLIENT_ATTRIBUTE)).isEqualTo("app");
        assertThat(session.getAttribute(AppOAuth2Service.APP_REDIRECT_URI_ATTRIBUTE)).isEqualTo(APP_URI);
        assertThat(session.getAttribute(AppOAuth2Service.APP_CODE_CHALLENGE_ATTRIBUTE)).isEqualTo(challenge(VERIFIER));
        assertThat(session.getAttribute(OAuth2LoginModeFilter.SESSION_ATTRIBUTE)).isEqualTo("login");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "quoteme://evil, S256",                    // 미허용 URI
            "quoteme://oauth2/callback/, S256",        // 정확히 일치하지 않음
            "quoteme://oauth2/callback?x=1, S256",
            "NULL, S256",                              // URI 누락
            "quoteme://oauth2/callback, plain",        // plain 방식
            "quoteme://oauth2/callback, NULL"          // 방식 누락
    }, nullValues = "NULL")
    @DisplayName("시작: 미허용 URI·잘못된 PKCE → 리다이렉트 없이 400-31")
    void start_invalid_400(String redirectUri, String method) throws Exception {
        MockHttpSession session = new MockHttpSession();

        start(redirectUri, challenge(VERIFIER), method, session)
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.message").value(startsWith("400-31")));
        assertThat(session.getAttribute(AppOAuth2Service.APP_CLIENT_ATTRIBUTE)).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL", "short", "has+plus/slash=pad-has+plus/slash=pad-has+plus/slash"}, nullValues = "NULL")
    @DisplayName("시작: challenge 누락·형식 오류 → 400-31")
    void start_badChallenge_400(String codeChallenge) throws Exception {
        start(APP_URI, codeChallenge, "S256", new MockHttpSession())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-31")));
    }

    @Test
    @DisplayName("시작: client 없는 요청은 기존 웹 흐름 그대로, 남아 있던 앱 속성은 제거")
    void start_web_clearsAppSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        AppOAuth2Service.startAppSession(session, APP_URI, challenge(VERIFIER), false);

        mvc.perform(get("/oauth2/authorization/google").param("mode", "login").session(session))
                .andExpect(status().isFound());

        assertThat(session.getAttribute(AppOAuth2Service.APP_CLIENT_ATTRIBUTE)).isNull();
        assertThat(session.getAttribute(OAuth2LoginModeFilter.SESSION_ATTRIBUTE)).isEqualTo("login");
    }

    @Test
    @DisplayName("시작: 허용 목록이 비어 있으면 앱 요청은 모두 400")
    void emptyAllowList_rejectsAll() {
        Object original = ReflectionTestUtils.getField(appOAuth2Service, "appRedirectUris");
        try {
            ReflectionTestUtils.setField(appOAuth2Service, "appRedirectUris", "");
            assertThat(appOAuth2Service.isAllowedRedirectUri(APP_URI)).isFalse();
            assertThat(appOAuth2Service.isAllowedRedirectUri("")).isFalse();
        } finally {
            ReflectionTestUtils.setField(appOAuth2Service, "appRedirectUris", original);
        }
    }

    @Test
    @DisplayName("성공 핸들러 앱 분기: 쿠키·토큰 없이 code만 붙여 앱 URI로, DB에는 해시만 저장")
    void successHandler_app() throws Exception {
        Rq rq = mock(Rq.class);
        String refreshBefore = member.getRefreshToken();

        String url = succeedAsApp(member, false, rq);

        assertThat(url).startsWith(APP_URI + "?code=").doesNotContain("&").doesNotContain("accessToken");
        String code = codeFrom(url);
        verifyNoInteractions(rq);
        assertThat(member.getRefreshToken()).isEqualTo(refreshBefore);

        List<OAuth2ExchangeCode> rows = exchangeCodeRepository.findAll();
        assertThat(rows).hasSize(1);
        OAuth2ExchangeCode row = rows.get(0);
        assertThat(row.getCodeHash()).isEqualTo(AppOAuth2Service.sha256Hex(code)).isNotEqualTo(code);
        assertThat(row.getPurpose()).isEqualTo(OAuth2ExchangeCode.Purpose.LOGIN);
        assertThat(row.getMemberId()).isEqualTo(member.getId());
        assertThat(row.getCodeChallenge()).isEqualTo(challenge(VERIFIER));
        assertThat(row.getUsedAt()).isNull();
        assertThat(row.getExpiresAt()).isBetween(LocalDateTime.now().plusSeconds(50), LocalDateTime.now().plusSeconds(61));
    }

    @Test
    @DisplayName("실패 핸들러 앱 분기: 기존 사유를 reason으로 붙여 앱 URI로, 세션 없으면 웹 실패 URI")
    void failureHandler_app() throws Exception {
        OAuth2FailureHandler handler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(handler, "failureUri", "http://front.test/login?error=oauth2");

        MockHttpServletRequest request = appSessionRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(new OAuth2Error(MemberService.SOCIAL_ACCOUNT_NOT_FOUND)));
        assertThat(response.getRedirectedUrl()).isEqualTo(APP_URI + "?reason=account_not_found");
        assertThat(request.getSession().getAttribute(AppOAuth2Service.APP_CLIENT_ATTRIBUTE)).isNull();

        MockHttpServletRequest unknown = appSessionRequest();
        MockHttpServletResponse unknownResponse = new MockHttpServletResponse();
        handler.onAuthenticationFailure(unknown, unknownResponse, new OAuth2AuthenticationException("boom"));
        assertThat(unknownResponse.getRedirectedUrl()).isEqualTo(APP_URI + "?reason=oauth2");

        MockHttpServletResponse webResponse = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), webResponse,
                new OAuth2AuthenticationException(new OAuth2Error(MemberService.SOCIAL_EMAIL_NOT_VERIFIED)));
        assertThat(webResponse.getRedirectedUrl()).isEqualTo("http://front.test/login?error=oauth2&reason=email_not_verified");
    }

    @Test
    @DisplayName("교환: 정상 → access·refresh·isNewUser·profileCompleted·item, 쿠키 없음, 토큰 동작")
    void exchange_ok() throws Exception {
        String code = codeFrom(succeedAsApp(member, false, mock(Rq.class)));

        MvcResult result = exchange(code, VERIFIER)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.isNewUser").value(false))
                .andExpect(jsonPath("$.data.profileCompleted").value(true))
                .andExpect(jsonPath("$.data.item.id").value(member.getId()))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.read(body, "$.data.accessToken");
        String refreshToken = com.jayway.jsonpath.JsonPath.read(body, "$.data.refreshToken");
        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").header("X-Client-Type", "app").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());
        assertThat(exchangeCodeRepository.findAll().get(0).getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("교환: 재사용 → 400-30")
    void exchange_reuse_400() throws Exception {
        String code = codeFrom(succeedAsApp(member, false, mock(Rq.class)));
        exchange(code, VERIFIER).andExpect(status().isOk());

        expectInvalidCode(exchange(code, VERIFIER));
    }

    @Test
    @DisplayName("교환: 61초 후(만료) → 400-30")
    void exchange_expired_400() throws Exception {
        String code = codeFrom(succeedAsApp(member, false, mock(Rq.class)));
        em.flush();
        em.createQuery("UPDATE OAuth2ExchangeCode c SET c.expiresAt = :t")
                .setParameter("t", LocalDateTime.now().minusSeconds(1))
                .executeUpdate();
        em.clear();

        expectInvalidCode(exchange(code, VERIFIER));
    }

    @Test
    @DisplayName("교환: verifier 불일치·없는 코드·빈 본문 → 같은 400-30")
    void exchange_mismatch_400() throws Exception {
        String code = codeFrom(succeedAsApp(member, false, mock(Rq.class)));

        expectInvalidCode(exchange(code, VERIFIER + "x"));
        expectInvalidCode(exchange("unknown-code", VERIFIER));
        expectInvalidCode(mvc.perform(post("/api/auth/oauth2/token").contentType(MediaType.APPLICATION_JSON).content("{}")));
        assertThat(exchangeCodeRepository.findAll().get(0).getUsedAt()).isNull();
    }

    @Test
    @DisplayName("신규 소셜 회원: isNewUser=true, profileCompleted=false (URL에는 isNewUser 없음)")
    void exchange_newUser() throws Exception {
        Member newMember = memberService.findOrCreateSocialMember(
                MemberProvider.GOOGLE, "app-new-1", "app-new@test.com", true, "구글닉", null, true).member();

        String url = succeedAsApp(newMember, true, mock(Rq.class));
        assertThat(url).doesNotContain("isNewUser");

        exchange(codeFrom(url), VERIFIER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.profileCompleted").value(false))
                .andExpect(jsonPath("$.data.item.profileCompleted").value(false));
    }
}
