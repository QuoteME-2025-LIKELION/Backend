package com.ll.demo.global.security.oauth2;

import com.ll.demo.domain.member.member.entity.Member;
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

    private final AuthTokenService authTokenService;
    private final Rq rq;

    @Value("${custom.oauth2.redirect-uri:http://localhost:5173/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = oAuth2User.getMember();

        // 1. 기존에 작성하셨던 AuthTokenService의 올바른 토큰 생성 메서드를 직접 호출합니다.
        // 만약 genToken이 아니라 다른 메서드명(예: genAccessToken)을 사용한다면 컴파일 전에 가볍게 이름만 맞춰주세요.
        String accessToken = authTokenService.genToken(member, 60 * 30); 
        String refreshToken = member.getRefreshToken(); // Member 엔티티 내부 필드에서 추출 혹은 필요시 생성 호출

        // 2. Rq 표준 쿠키 인프라를 통해 브라우저에 토큰 저장
        rq.setCookie("accessToken", accessToken, 60 * 30);
        if (refreshToken != null) {
            rq.setCookie("refreshToken", refreshToken, 60 * 60 * 24 * 7);
        }

        // 3. 프론트엔드가 주소창에서 정상적으로 파싱할 수 있도록 안전하게 변수 바인딩 마감
        String debugRedirectUri = redirectUri + "?accessToken=" + accessToken;
        if (refreshToken != null) {
            debugRedirectUri += "&refreshToken=" + refreshToken;
        }
        
        getRedirectStrategy().sendRedirect(request, response, debugRedirectUri);
    }
}
