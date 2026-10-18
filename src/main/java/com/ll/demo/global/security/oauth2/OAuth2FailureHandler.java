package com.ll.demo.global.security.oauth2;

import com.ll.demo.domain.member.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Value("${custom.oauth2.failure-uri:http://localhost:5173/login?error=oauth2}")
    private String failureUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 에러 로그 확인
        log.error("OAuth2 로그인 실패: {}", exception.getMessage(), exception);
        getRedirectStrategy().sendRedirect(request, response, resolveFailureUri(exception));
    }

    private String resolveFailureUri(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String code = oauth2Exception.getError().getErrorCode();
            if (MemberService.SOCIAL_ACCOUNT_NOT_FOUND.equals(code)
                    || MemberService.SOCIAL_EMAIL_NOT_VERIFIED.equals(code)) {
                return UriComponentsBuilder.fromUriString(failureUri).queryParam("reason", code).build().toUriString();
            }
        }
        return failureUri;
    }
}