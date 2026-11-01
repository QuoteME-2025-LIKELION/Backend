package com.ll.demo.global.security.oauth2;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.repository.MemberSocialAccountRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.member.member.type.MemberProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestOperations;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

// 외부 호출은 RestOperations 목으로 대체
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OAuth2AccountLinkFlowTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberSocialAccountRepository socialAccountRepository;
    @Autowired private ClientRegistrationRepository clientRegistrationRepository;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private OAuth2MemberService service;
    private Member me;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        me = memberRepository.save(Member.builder().email("me@test.com").nickname("나").password("password123").build());
        service = new OAuth2MemberService(memberService, request);
        RestOperations rest = mock(RestOperations.class);
        given(rest.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .willReturn(ResponseEntity.ok(Map.of("id", 777L, "kakao_account", Map.of(
                        "email", "kakao@test.com", "is_email_valid", true, "is_email_verified", true))));
        service.setRestOperations(rest);
    }

    private CustomOAuth2User loadKakaoUser() {
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token",
                Instant.now(), Instant.now().plusSeconds(60));
        return (CustomOAuth2User) service.loadUser(
                new OAuth2UserRequest(clientRegistrationRepository.findByRegistrationId("kakao"), token));
    }

    @Test
    @DisplayName("링크 속성이 있으면 현재 회원에 연동하고 속성을 즉시 제거, 로그인 회원은 그대로")
    void linkFlow() {
        request.getSession().setAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE, me.getId());

        CustomOAuth2User user = loadKakaoUser();

        assertThat(user.getMember().getId()).isEqualTo(me.getId());
        assertThat(user.isNewUser()).isFalse();
        assertThat(request.getSession().getAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE)).isNull();
        assertThat(request.getAttribute(OAuth2MemberService.LINKED_PROVIDER_ATTRIBUTE)).isEqualTo("kakao");
        assertThat(socialAccountRepository.findByProviderAndProviderId(MemberProvider.KAKAO, "777").orElseThrow()
                .getMember().getId()).isEqualTo(me.getId());
    }

    @Test
    @DisplayName("링크 속성이 없으면 일반 로그인이고 연동 표시도 없다")
    void normalLogin() {
        CustomOAuth2User user = loadKakaoUser();

        assertThat(user.isNewUser()).isTrue();
        assertThat(request.getAttribute(OAuth2MemberService.LINKED_PROVIDER_ATTRIBUTE)).isNull();
    }

    @Test
    @DisplayName("연동 실패에도 링크 속성은 제거되어 다음 로그인이 연동으로 처리되지 않는다")
    void linkFailure_consumesAttribute() {
        Member other = memberRepository.save(Member.builder().email("o@test.com").nickname("남").password("password123").build());
        memberService.linkSocialAccount(other.getId(), MemberProvider.KAKAO, "777");
        request.getSession().setAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE, me.getId());

        OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class, this::loadKakaoUser);

        assertThat(ex.getError().getErrorCode()).isEqualTo("already_linked");
        assertThat(request.getSession().getAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE)).isNull();
        assertThat(request.getAttribute(OAuth2MemberService.LINKED_PROVIDER_ATTRIBUTE)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"already_linked", "provider_already_linked"})
    @DisplayName("실패 핸들러가 연동 실패 reason을 전달한다")
    void failureHandler_passesReason(String code) throws Exception {
        OAuth2FailureHandler handler = new OAuth2FailureHandler();
        ReflectionTestUtils.setField(handler, "failureUri", "http://front.test/login?error=oauth2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new OAuth2AuthenticationException(new OAuth2Error(code)));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://front.test/login?error=oauth2&reason=" + code);
    }
}
