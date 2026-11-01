package com.ll.demo.global.security.oauth2;

import com.ll.demo.AppConfig;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.standard.rq.Rq;
import com.ll.demo.global.security.AuthTokenService; // 패키지 구조에 맞게 주입
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final int REFRESH_TOKEN_COOKIE_SEC = 60 * 60 * 24 * 30;

    private final AuthTokenService authTokenService;
    private final MemberService memberService;
    private final Rq rq;

    @Value("${custom.oauth2.redirect-uri:http://localhost:5173/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = oAuth2User.getMember();

        int accessTokenExpirationSec = AppConfig.getAccessTokenExpirationSec();
        String accessToken = authTokenService.genToken(member, accessTokenExpirationSec);
        String refreshToken = memberService.genRefreshToken(member);

        rq.setCookie("accessToken", accessToken, accessTokenExpirationSec);
        rq.setCookie("refreshToken", refreshToken, REFRESH_TOKEN_COOKIE_SEC);

        // 과도기: access 토큰만 URL로 전달, refresh 토큰은 쿠키로만 전달
        String targetUrl = redirectUri + "?accessToken=" + accessToken;
        if (oAuth2User.isNewUser()) {
            targetUrl += "&isNewUser=true";
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
