package com.ll.demo.global.security;

import com.ll.demo.AppConfig;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.standard.dto.util.Ut;
import com.ll.demo.standard.rq.Rq;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final MemberService memberService;
    private final AuthTokenService authTokenService;
    private final Rq rq;

    @Override
    @SneakyThrows
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String accessToken = null;
        String refreshToken = null;
        boolean cookieBased = true;

        String authorization = req.getHeader("Authorization"); // 수정 - 요청 헤더 형식을 코드에 맞추기
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            String[] bits = token.split(" ");
            if (bits.length == 2) {
                refreshToken = bits[0];
                accessToken = bits[1];
            } else {
                accessToken = bits[0]; // 토큰이 하나면 Access Token으로 간주
            }
            cookieBased = false;
        }
        if (Ut.str.isBlank(accessToken) || Ut.str.isBlank(refreshToken)) {
            accessToken = rq.getCookieValue("accessToken", "");
            refreshToken = rq.getCookieValue("refreshToken", "");
            cookieBased = true;
        }
        if (Ut.str.isBlank(accessToken) && Ut.str.isBlank(refreshToken)) {
            filterChain.doFilter(req, response);
            return;
        }

        if (!authTokenService.validateToken(accessToken)) {
            if (Ut.str.isNotBlank(refreshToken)) {
                Member member = memberService.findByRefreshToken(refreshToken).orElse(null);

                if (member != null) {
                    int expSec = AppConfig.getAccessTokenExpirationSec();
                    String newAccessToken = authTokenService.genToken(member, expSec);

                    if (cookieBased)
                        rq.setCookie(response, "accessToken", newAccessToken, expSec);
                    else
                        response.setHeader("Authorization", "Bearer " + refreshToken + " " + newAccessToken);

                    accessToken = newAccessToken;
                }
            }
        }

        if (Ut.str.isNotBlank(accessToken) && authTokenService.validateToken(accessToken)) {
            Map<String, Object> accessTokenData = authTokenService.getDataFrom(accessToken);
            long id = Long.parseLong(accessTokenData.get("id").toString());

            // 1. DB에서 실제 회원 조회 (SecurityUser에는 Member 객체가 통째로 필요함)
            Member member = (Member) memberService.findById(id).orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

            // 2. SecurityUser 생성 (VIP 티켓 발급)
            // (비밀번호는 공란 "", 권한은 member.getAuthorities() 사용)
            SecurityUser securityUser = new SecurityUser(
                    member,
                    member.getUsername(),
                    "",
                    member.getAuthorities()
            );

            // 3. 인증 객체에 SecurityUser 넣기
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    securityUser,
                    null,
                    securityUser.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(req, response);
    }
}