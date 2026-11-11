package com.ll.demo.global.security.oauth2;

import com.ll.demo.global.rsData.RsData;
import com.ll.demo.standard.dto.util.Ut;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class OAuth2LoginModeFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTRIBUTE = "OAUTH2_LOGIN_MODE";
    public static final String MODE_LOGIN = "login";
    public static final String MODE_SIGNUP = "signup";

    private static final RequestMatcher AUTHORIZATION_REQUEST_MATCHER = new AntPathRequestMatcher("/oauth2/authorization/**");

    private final AppOAuth2Service appOAuth2Service;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AUTHORIZATION_REQUEST_MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String mode = request.getParameter("mode");
        HttpSession session = request.getSession();

        // 앱 로그인(client=app): 복귀 URI 허용 목록·PKCE를 검사하고, 실패하면 리다이렉트하지 않음
        if (AppOAuth2Service.CLIENT_APP.equals(request.getParameter("client"))) {
            String redirectUri = request.getParameter("redirect_uri");
            String codeChallenge = request.getParameter("code_challenge");
            if (!appOAuth2Service.isValidAppRequest(redirectUri, codeChallenge, request.getParameter("code_challenge_method"))) {
                AppOAuth2Service.clearAppSession(session);
                response.setStatus(400);
                response.setContentType("application/json;charset=UTF-8");
                // 응답 형식은 GlobalException과 같음 (message가 "400-31 - "로 시작)
                response.getWriter().write(Ut.json.toString(RsData.of(
                        AppOAuth2Service.INVALID_APP_REQUEST + " - " + AppOAuth2Service.INVALID_APP_REQUEST_MESSAGE, null)));
                return;
            }
            AppOAuth2Service.startAppSession(session, redirectUri, codeChallenge, false);
            session.removeAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE);
        } else if (mode != null || !AppOAuth2Service.isAppLinkSession(session)) {
            // 웹 요청: 앱 속성을 지움. 앱 연동(/oauth2/link에서 넘어옴)은 client·mode 없이 들어오므로 유지
            AppOAuth2Service.clearAppSession(session);
        }

        if (MODE_LOGIN.equals(mode) || MODE_SIGNUP.equals(mode)) {
            session.setAttribute(SESSION_ATTRIBUTE, mode);
            // 중단된 연동이 남아 있어도 명시적인 로그인·가입이 연동으로 처리되지 않게 함 (연동 시작은 mode 없이 들어옴)
            session.removeAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE);
        } else {
            session.removeAttribute(SESSION_ATTRIBUTE);
        }
        filterChain.doFilter(request, response);
    }
}