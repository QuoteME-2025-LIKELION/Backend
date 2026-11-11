package com.ll.demo.standard.rq;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import java.util.Arrays;

@Component
@RequestScope
@RequiredArgsConstructor
public class Rq {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final MemberRepository memberRepository;
    private final AuthTokenService authTokenService;

    private Member cachedMember = null;
    private boolean memberResolved = false;

    public Member getMember() {
        if (memberResolved) {
            return cachedMember;
        }
        memberResolved = true;

        String accessToken = getCookieValue("accessToken", null);
        if (accessToken == null || !authTokenService.validateAccessToken(accessToken)) {
            return null;
        }

        try {
            Long memberId = Long.valueOf(authTokenService.getDataFrom(accessToken).get("id").toString());
            cachedMember = memberRepository.findById(memberId).orElse(null);
        } catch (Exception e) {
            cachedMember = null;
        }

        return cachedMember;
    }

    public Member getMemberOrThrow() {
        Member member = getMember();
        if (member == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return member;
    }

    public Long getMemberId() {
        return getMemberOrThrow().getId();
    }

    // 표준 쿠키 설정 (SameSite=None, Secure=true)
    public void setCookie(String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 하위 호환성을 유지하기 위한 헬퍼 핸들러 메서드
    @Deprecated
    public void setCookie(HttpServletResponse ignoredResponse, String name, String value, int maxAge) {
        setCookie(name, value, maxAge);
    }

    public void deleteCookie(String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String getCookieValue(String name, String defaultValue) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return defaultValue;
        }
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(defaultValue);
    }

    // 앱 요청 식별 헤더: 값이 app이면 refresh 토큰을 본문으로 주고받고 토큰 쿠키를 쓰지 않음
    public static final String CLIENT_TYPE_HEADER = "X-Client-Type";

    public boolean isAppClient() {
        return "app".equalsIgnoreCase(request.getHeader(CLIENT_TYPE_HEADER));
    }

    public String getCurrentUrlPath() {
        return request.getRequestURI();
    }
}
