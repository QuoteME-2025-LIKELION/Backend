package com.ll.demo.global.security;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TokenTypeTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberService memberService;
    @Autowired private AuthTokenService authTokenService;

    @Value("${custom.secret.jwt.secretKey}")
    private String secretKey;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .email("typ@test.com").nickname("유형").password("password123").build());
    }

    // 배포 전 형식: typ 없이 id만 담긴 토큰
    private String legacyToken(int expirationSec) {
        return Jwts.builder()
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS512)
                .setExpiration(Date.from(Instant.now().plusSeconds(expirationSec)))
                .claim("id", member.getId())
                .compact();
    }

    @Test
    @DisplayName("access·refresh 토큰에 typ 클레임이 들어간다")
    void tokensCarryType() {
        assertThat(authTokenService.getDataFrom(authTokenService.genToken(member, 600)).get("typ")).isEqualTo("access");
        assertThat(authTokenService.getDataFrom(memberService.genRefreshToken(member)).get("typ")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("refresh 토큰을 Bearer access로 보내면 미인증")
    void refreshTokenAsBearer_isUnauthenticated() throws Exception {
        String refreshToken = memberService.genRefreshToken(member);

        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("access 토큰을 refresh 엔드포인트에 보내면 401-3")
    void accessTokenAsRefresh_is401_3() throws Exception {
        String accessToken = authTokenService.genToken(member, 600);
        member.setRefreshToken(accessToken);
        memberRepository.saveAndFlush(member);

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(startsWith("401-3")));
    }

    @Test
    @DisplayName("로그아웃 뒤 이전 refresh JWT를 Bearer로 보내도 미인증")
    void loggedOutRefreshAsBearer_isUnauthenticated() throws Exception {
        String refreshToken = memberService.genRefreshToken(member);
        mvc.perform(post("/api/auth/logout").cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("과도기: typ 없는 구 refresh 토큰은 DB와 일치하면 재발급에 쓸 수 있다")
    void legacyRefreshToken_worksForRefresh() throws Exception {
        String legacyRefresh = legacyToken(60 * 60 * 24 * 30);
        member.setRefreshToken(legacyRefresh);
        memberRepository.saveAndFlush(member);

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", legacyRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("typ 없는 구 토큰은 Bearer access로 받지 않는다")
    void legacyTokenAsBearer_isUnauthenticated() throws Exception {
        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + legacyToken(600)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("웹: typ 없는 구 access 쿠키 + 유효 refresh 쿠키면 필터가 재발급해 재로그인 없이 200")
    void legacyAccessCookie_isReissuedByRefreshCookie() throws Exception {
        String refreshToken = memberService.genRefreshToken(member);

        mvc.perform(get("/api/profile")
                        .cookie(new Cookie("accessToken", legacyToken(600)), new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", org.hamcrest.Matchers.hasItem(startsWith("accessToken="))));
    }

    @Test
    @DisplayName("새 로그인으로 refresh가 바뀌면 이전 refresh는 재발급에 쓸 수 없다")
    void newLogin_invalidatesPreviousRefresh() throws Exception {
        String first = memberService.genRefreshToken(member);
        String second = memberService.genRefreshToken(member);

        assertThat(second).isNotEqualTo(first);
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", first)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", second)))
                .andExpect(status().isOk());
    }
}
