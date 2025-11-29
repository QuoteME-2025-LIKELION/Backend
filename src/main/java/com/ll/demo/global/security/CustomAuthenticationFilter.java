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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

        String authorization = req.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String[] authorizationBits = authorization.substring("Bearer ".length()).split(" ", 2);
            if (authorizationBits.length == 2) {
                refreshToken = authorizationBits[0];
                accessToken = authorizationBits[1];
                cookieBased = false;
            }
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

            User user = new User(id + "", "", List.of());
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(req, response);
    }
}