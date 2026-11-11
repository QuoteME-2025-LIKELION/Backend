package com.ll.demo.global.security.oauth2;

import com.jayway.jsonpath.JsonPath;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.entity.MemberSocialAccount;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.repository.MemberSocialAccountRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.member.member.type.MemberProvider;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.standard.rq.Rq;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 외부(카카오) 호출은 RestOperations 목으로 대체
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppAccountLinkTest {

    private static final String LINK_URI = "quoteme://oauth2/link";
    private static final String VERIFIER = AppOAuth2LoginTest.VERIFIER;

    @Autowired private MockMvc mvc;
    @Autowired private AppOAuth2Service appOAuth2Service;
    @Autowired private OAuth2ExchangeCodeRepository exchangeCodeRepository;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberSocialAccountRepository socialAccountRepository;
    @Autowired private ClientRegistrationRepository clientRegistrationRepository;
    @Autowired private EntityManager em;

    private Member me;

    @BeforeEach
    void setUp() {
        me = memberRepository.save(Member.builder().email("link-me@test.com").nickname("나").password("password123").build());
    }

    private String bearer(Member member) {
        return "Bearer " + authTokenService.genToken(member, 600);
    }

    private ResultActions startLink(Member as, String provider, String body) throws Exception {
        return mvc.perform(post("/api/profile/account/link/" + provider + "/start")
                .header("Authorization", bearer(as))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String validStartBody() throws Exception {
        return "{\"redirectUri\":\"" + LINK_URI + "\",\"codeChallenge\":\"" + AppOAuth2LoginTest.challenge(VERIFIER)
                + "\",\"codeChallengeMethod\":\"S256\"}";
    }

    private String ticketFromStart() throws Exception {
        String body = startLink(me, "kakao", validStartBody()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(body, "$.authorizationUrl");
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("ticket");
    }

    private ResultActions enter(String ticket, MockHttpSession session) throws Exception {
        return mvc.perform(get("/oauth2/link").param("ticket", ticket).param("provider", "kakao").session(session));
    }

    @SuppressWarnings("unchecked")
    private CustomOAuth2User loadKakaoUser(MockHttpServletRequest request, long kakaoId) {
        OAuth2MemberService service = new OAuth2MemberService(memberService, request);
        RestOperations rest = mock(RestOperations.class);
        given(rest.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .willReturn(org.springframework.http.ResponseEntity.ok(Map.of("id", kakaoId, "kakao_account", Map.of(
                        "email", "kakao-link@test.com", "is_email_valid", true, "is_email_verified", true))));
        service.setRestOperations(rest);
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token",
                Instant.now(), Instant.now().plusSeconds(60));
        return (CustomOAuth2User) service.loadUser(
                new OAuth2UserRequest(clientRegistrationRepository.findByRegistrationId("kakao"), token));
    }

    // 시작 → 브라우저 진입 → 소셜 인증 성공 → 앱 복귀 URL
    private String linkThroughBrowser(long kakaoId) throws Exception {
        MockHttpSession session = new MockHttpSession();
        enter(ticketFromStart(), session).andExpect(status().isFound());

        MockHttpServletRequest callback = new MockHttpServletRequest();
        callback.setSession(session);
        CustomOAuth2User user = loadKakaoUser(callback, kakaoId);

        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(authTokenService, memberService, mock(Rq.class), appOAuth2Service);
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(callback, response, new TestingAuthenticationToken(user, null));
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
        return response.getRedirectedUrl();
    }

    private ResultActions confirm(Member as, String code, String verifier) throws Exception {
        return mvc.perform(post("/api/profile/account/link/confirm")
                .header("Authorization", bearer(as))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"codeVerifier\":\"" + verifier + "\"}"));
    }

    private static String param(String url, String name) {
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst(name);
    }

    private boolean kakaoLinked(Member member) {
        em.flush();
        em.clear();
        return socialAccountRepository.findAllByMemberOrderById(memberRepository.findById(member.getId()).orElseThrow())
                .stream().anyMatch(a -> a.getProvider() == MemberProvider.KAKAO);
    }

    @Test
    @DisplayName("시작(Bearer) → 200 + 연동 URL, 티켓은 해시로 5분 저장")
    void start_ok() throws Exception {
        String body = startLink(me, "kakao", validStartBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").value(startsWith("http://localhost:8070/oauth2/link?ticket=")))
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(body, "$.authorizationUrl");
        assertThat(url).endsWith("&provider=kakao");

        OAuth2ExchangeCode row = exchangeCodeRepository.findAll().get(0);
        assertThat(row.getPurpose()).isEqualTo(OAuth2ExchangeCode.Purpose.LINK_TICKET);
        assertThat(row.getCodeHash()).isEqualTo(AppOAuth2Service.sha256Hex(param(url, "ticket")));
        assertThat(row.getMemberId()).isEqualTo(me.getId());
        assertThat(row.getRedirectUri()).isEqualTo(LINK_URI);
        assertThat(row.getExpiresAt()).isAfter(LocalDateTime.now().plusSeconds(290));
    }

    @Test
    @DisplayName("시작: 미허용 URI·plain → 400-31, 게스트 403, 미지원 provider 400, 토큰 없음 401")
    void start_rejections() throws Exception {
        startLink(me, "kakao", "{\"redirectUri\":\"quoteme://evil\",\"codeChallenge\":\""
                + AppOAuth2LoginTest.challenge(VERIFIER) + "\",\"codeChallengeMethod\":\"S256\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-31")));
        startLink(me, "kakao", "{\"redirectUri\":\"" + LINK_URI + "\",\"codeChallenge\":\""
                + AppOAuth2LoginTest.challenge(VERIFIER) + "\",\"codeChallengeMethod\":\"plain\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-31")));
        startLink(memberService.findOrCreateGuest(), "kakao", validStartBody()).andExpect(status().isForbidden());
        startLink(me, "naver", validStartBody()).andExpect(status().isBadRequest());
        mvc.perform(post("/api/profile/account/link/kakao/start").contentType(MediaType.APPLICATION_JSON).content(validStartBody()))
                .andExpect(status().isUnauthorized());
        assertThat(exchangeCodeRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("브라우저 진입: 302로 소셜 인증, 세션에 연동 대상 저장, client 없이 인가 요청해도 유지")
    void enter_setsSessionAndSurvivesFilter() throws Exception {
        MockHttpSession session = new MockHttpSession();

        enter(ticketFromStart(), session)
                .andExpect(status().isFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/oauth2/authorization/kakao"));
        mvc.perform(get("/oauth2/authorization/kakao").session(session)).andExpect(status().isFound());

        assertThat(session.getAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE)).isEqualTo(me.getId());
        assertThat(session.getAttribute(AppOAuth2Service.APP_LINK_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertThat(session.getAttribute(AppOAuth2Service.APP_REDIRECT_URI_ATTRIBUTE)).isEqualTo(LINK_URI);
    }

    @Test
    @DisplayName("브라우저 진입: 티켓 재사용·만료·provider 불일치·없음 → 400-32")
    void enter_invalidTicket_400() throws Exception {
        String ticket = ticketFromStart();
        enter(ticket, new MockHttpSession()).andExpect(status().isFound());
        enter(ticket, new MockHttpSession())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-32")));

        String expired = ticketFromStart();
        em.flush();
        em.createQuery("UPDATE OAuth2ExchangeCode c SET c.expiresAt = :t WHERE c.usedAt IS NULL")
                .setParameter("t", LocalDateTime.now().minusSeconds(1)).executeUpdate();
        em.clear();
        enter(expired, new MockHttpSession())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-32")));

        mvc.perform(get("/oauth2/link").param("ticket", ticketFromStart()).param("provider", "google"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-32")));
        mvc.perform(get("/oauth2/link"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-32")));
    }

    @Test
    @DisplayName("연동 성공: 확정 전에는 미연결, 확정 후 연결되고 linkedAccounts 반환, 재확정 400-30")
    void link_confirm() throws Exception {
        String url = linkThroughBrowser(9001L);

        assertThat(url).startsWith(LINK_URI + "?code=").endsWith("&linked=kakao");
        assertThat(kakaoLinked(me)).isFalse();

        String code = param(url, "code");
        confirm(me, code, VERIFIER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedAccounts[0].provider").value("KAKAO"));
        assertThat(kakaoLinked(me)).isTrue();

        confirm(me, code, VERIFIER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-30")));
    }

    @Test
    @DisplayName("확정: 다른 회원 토큰·verifier 불일치 → 400-30, 연결되지 않음")
    void confirm_rejections() throws Exception {
        String code = param(linkThroughBrowser(9002L), "code");
        Member other = memberRepository.save(Member.builder().email("link-other@test.com").nickname("남").password("password123").build());

        confirm(other, code, VERIFIER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-30")));
        confirm(me, code, VERIFIER + "x")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("400-30")));

        assertThat(kakaoLinked(me)).isFalse();
        assertThat(kakaoLinked(other)).isFalse();
    }

    @Test
    @DisplayName("다른 회원에게 연결된 소셜 계정: 앱 URI로 reason=already_linked")
    void alreadyLinked_reason() throws Exception {
        Member owner = memberRepository.save(Member.builder().email("link-owner@test.com").nickname("주인").password("password123").build());
        memberService.linkSocialAccount(owner.getId(), MemberProvider.KAKAO, "9003");

        MockHttpSession session = new MockHttpSession();
        enter(ticketFromStart(), session).andExpect(status().isFound());
        MockHttpServletRequest callback = new MockHttpServletRequest();
        callback.setSession(session);
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                () -> loadKakaoUser(callback, 9003L));

        OAuth2FailureHandler handler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(handler, "failureUri", "http://front.test/login?error=oauth2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(callback, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo(LINK_URI + "?reason=already_linked");
        assertThat(session.getAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE)).isNull();
        assertThat(session.getAttribute(AppOAuth2Service.APP_CLIENT_ATTRIBUTE)).isNull();
    }

    @Test
    @DisplayName("확정 시점에 중복 제한 재검사: 그 사이 다른 회원이 연결했으면 409-2")
    void confirm_rechecksConflict() throws Exception {
        String code = param(linkThroughBrowser(9004L), "code");
        Member owner = memberRepository.save(Member.builder().email("link-race@test.com").nickname("선점").password("password123").build());
        socialAccountRepository.save(MemberSocialAccount.builder().member(owner).provider(MemberProvider.KAKAO).providerId("9004").build());

        confirm(me, code, VERIFIER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(startsWith("409-2")));
        assertThat(kakaoLinked(me)).isFalse();
    }
}
