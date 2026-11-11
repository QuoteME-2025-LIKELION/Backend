package com.ll.demo.global.security.oauth2;

import com.ll.demo.domain.member.member.dto.SocialLoginResult;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.member.member.type.MemberProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2MemberService extends DefaultOAuth2UserService {

    public static final String LINK_SESSION_ATTRIBUTE = "OAUTH2_LINK_MEMBER_ID";
    public static final String LINKED_PROVIDER_ATTRIBUTE = "OAUTH2_LINKED_PROVIDER";
    public static final String LINKED_PROVIDER_ID_ATTRIBUTE = "OAUTH2_LINKED_PROVIDER_ID";

    private final MemberService memberService;
    private final HttpServletRequest request;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        boolean appLink = AppOAuth2Service.isAppLinkSession(request.getSession(false));
        Long linkMemberId = consumeLinkMemberId();
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String providerId;
        String email;
        boolean emailVerified;
        String nickname;
        String profileImage;
        MemberProvider provider;

        if ("google".equals(registrationId)) {
            provider = MemberProvider.GOOGLE;
            providerId = String.valueOf(attributes.get("sub"));
            email = (String) attributes.get("email");
            emailVerified = isTrue(attributes.get("email_verified"));
            nickname = (String) attributes.get("name");
            profileImage = (String) attributes.get("picture");
        } else if ("kakao".equals(registrationId)) {
            provider = MemberProvider.KAKAO;
            providerId = String.valueOf(attributes.get("id"));
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoProfile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
            email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
            emailVerified = kakaoAccount != null
                    && isTrue(kakaoAccount.get("is_email_valid"))
                    && isTrue(kakaoAccount.get("is_email_verified"));
            // 이메일 제공 안 하면 providerId 기반 가짜 이메일 생성
            if (email == null || email.isBlank()) {
                email = providerId + "@kakao.user";
                emailVerified = false;
            }
            nickname = kakaoProfile != null ? (String) kakaoProfile.get("nickname") : null;
            profileImage = kakaoProfile != null ? (String) kakaoProfile.get("profile_image_url") : null;
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + registrationId);
        }

        String loginMode = consumeLoginMode();
        if (linkMemberId != null && appLink) {
            // 앱 연동: 제한만 검사하고 저장은 확정 API에서 (티켓 유출 시 남의 소셜 계정이 연결되는 것을 막음)
            Member member = memberService.validateSocialLink(linkMemberId, provider, providerId);
            request.setAttribute(LINKED_PROVIDER_ATTRIBUTE, registrationId);
            request.setAttribute(LINKED_PROVIDER_ID_ATTRIBUTE, providerId);
            return new CustomOAuth2User(member, attributes, false);
        }
        if (linkMemberId != null) {
            Member member = memberService.linkSocialAccount(linkMemberId, provider, providerId);
            request.setAttribute(LINKED_PROVIDER_ATTRIBUTE, registrationId);
            return new CustomOAuth2User(member, attributes, false);
        }

        boolean createIfAbsent = !OAuth2LoginModeFilter.MODE_LOGIN.equals(loginMode);
        SocialLoginResult result = memberService.findOrCreateSocialMember(
                provider, providerId, email, emailVerified, nickname, profileImage, createIfAbsent);
        return new CustomOAuth2User(result.member(), attributes, result.newUser());
    }

    private boolean isTrue(Object value) {
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Long consumeLinkMemberId() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object memberId = session.getAttribute(LINK_SESSION_ATTRIBUTE);
        session.removeAttribute(LINK_SESSION_ATTRIBUTE);
        return memberId instanceof Long ? (Long) memberId : null;
    }

    private String consumeLoginMode() {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object mode = session.getAttribute(OAuth2LoginModeFilter.SESSION_ATTRIBUTE);
        session.removeAttribute(OAuth2LoginModeFilter.SESSION_ATTRIBUTE);
        return mode instanceof String ? (String) mode : null;
    }
}