package com.ll.demo.standard.rq;

import com.ll.demo.domain.member.member.entity.Member;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;

@Component
@RequestScope // 요청마다 http 빈 생성
@RequiredArgsConstructor
public class Rq {
    private final HttpServletRequest request;
    private Member member = null;

    public Member getMember() {
        if (member != null) {
            return member;
        }

        //
        return null;
    }

    public void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // ResponseCookie > 크로스 도메인 설정
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(true)      // SameSite=None
                .sameSite("None")  // 프론트 백 포트가 다를 때 필수 설정
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String getCookieValue(String name, String defaultValue) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> c.getName().equals(name))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(defaultValue);
        }
        return defaultValue;
    }

    public String getCurrentUrlPath() {
        return request.getRequestURI();
    }
}