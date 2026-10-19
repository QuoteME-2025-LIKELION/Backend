package com.ll.demo.global.security.oauth2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OAuth2LoginModeFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTRIBUTE = "OAUTH2_LOGIN_MODE";
    public static final String MODE_LOGIN = "login";
    public static final String MODE_SIGNUP = "signup";

    private static final RequestMatcher AUTHORIZATION_REQUEST_MATCHER = new AntPathRequestMatcher("/oauth2/authorization/**");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !AUTHORIZATION_REQUEST_MATCHER.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String mode = request.getParameter("mode");
        HttpSession session = request.getSession();
        if (MODE_LOGIN.equals(mode) || MODE_SIGNUP.equals(mode)) {
            session.setAttribute(SESSION_ATTRIBUTE, mode);
        } else {
            session.removeAttribute(SESSION_ATTRIBUTE);
        }
        filterChain.doFilter(request, response);
    }
}