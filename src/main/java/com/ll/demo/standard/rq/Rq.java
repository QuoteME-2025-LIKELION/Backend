package com.ll.demo.standard.rq;

import com.ll.demo.domain.member.member.entity.Member;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

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

    public void setCookie(HttpServletResponse response, String name, String value, int maxAge) { // 수정
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
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