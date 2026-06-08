package com.ll.demo.global.security.oauth2;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.member.member.type.MemberProvider;
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

    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String providerId;
        String email;
        String nickname;
        String profileImage;
        MemberProvider provider;

        if ("google".equals(registrationId)) {
            provider = MemberProvider.GOOGLE;
            providerId = String.valueOf(attributes.get("sub"));
            email = (String) attributes.get("email");
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
            // 이메일 제공 안 하면 providerId 기반 가짜 이메일 생성
            if (email == null || email.isBlank()) {
                email = providerId + "@kakao.user";
            }
            nickname = kakaoProfile != null ? (String) kakaoProfile.get("nickname") : null;
            profileImage = kakaoProfile != null ? (String) kakaoProfile.get("profile_image_url") : null;
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + registrationId);
        }

        Member member = memberService.findOrCreateSocialMember(provider, providerId, email, nickname, profileImage);
        return new CustomOAuth2User(member, attributes);
    }
}
