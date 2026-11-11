package com.ll.demo.global.security.oauth2;

import com.ll.demo.domain.member.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    static final String APP_UNKNOWN_REASON = "oauth2";

    @Value("${custom.oauth2.failure-uri:http://localhost:5173/login?error=oauth2}")
    private String failureUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 에러 로그 확인
        log.error("OAuth2 로그인 실패: {}", exception.getMessage(), exception);

        // 앱 인증이면 저장된 앱 URI로 사유를 붙여 복귀 (알 수 없는 실패는 reason=oauth2)
        HttpSession session = request.getSession(false);
        AppOAuth2Service.AppSession appSession = AppOAuth2Service.takeAppSession(session);
        if (appSession != null) {
            if (appSession.link()) {
                session.removeAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE);
            }
            String reason = resolveReason(exception);
            getRedirectStrategy().sendRedirect(request, response, UriComponentsBuilder.fromUriString(appSession.redirectUri())
                    .queryParam("reason", reason != null ? reason : APP_UNKNOWN_REASON).build().toUriString());
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, resolveFailureUri(exception));
    }

    private String resolveFailureUri(AuthenticationException exception) {
        String reason = resolveReason(exception);
        if (reason != null) {
            return UriComponentsBuilder.fromUriString(failureUri).queryParam("reason", reason).build().toUriString();
        }
        return failureUri;
    }

    private String resolveReason(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String code = oauth2Exception.getError().getErrorCode();
            if (MemberService.SOCIAL_ACCOUNT_NOT_FOUND.equals(code)
                    || MemberService.SOCIAL_EMAIL_NOT_VERIFIED.equals(code)
                    || MemberService.SOCIAL_ALREADY_LINKED.equals(code)
                    || MemberService.SOCIAL_PROVIDER_ALREADY_LINKED.equals(code)) {
                return code;
            }
        }
        return null;
    }
}