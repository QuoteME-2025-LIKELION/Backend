package com.ll.demo.global.security;

import com.ll.demo.domain.member.member.entity.Member;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    @Value("${custom.secret.jwt.secretKey}")
    private String secretKey;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public static final String TYPE_CLAIM = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    // 과도기: 배포 전에 발급된 typ 없는 refresh 토큰을 DB 일치로 허용 (기존 refresh 만료 후 false로 바꾸고 제거)
    public static final boolean ALLOW_UNTYPED_LEGACY_REFRESH = true;

    // Access Token 생성
    public String genToken(Member member, int expirationSec) {
        return Jwts.builder()
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .setExpiration(Date.from(Instant.now().plusSeconds(expirationSec)))
                .claim("id", member.getId())
                .claim(TYPE_CLAIM, TYPE_ACCESS)
                .compact();
    }

    // Refresh Token 생성 (jti로 같은 초에 다시 발급해도 이전 토큰과 구분)
    public String genRefreshToken(Member member, int expirationSec) {
        return Jwts.builder()
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .setExpiration(Date.from(Instant.now().plusSeconds(expirationSec)))
                .claim("id", member.getId())
                .claim(TYPE_CLAIM, TYPE_REFRESH)
                .setId(UUID.randomUUID().toString())
                .compact();
    }

    // access 자리에서 쓸 수 있는 토큰인지 (서명·만료 + typ=access)
    public boolean validateAccessToken(String token) {
        try {
            return TYPE_ACCESS.equals(getDataFrom(token).get(TYPE_CLAIM));
        } catch (Exception e) {
            return false;
        }
    }

    // refresh 용도로 받을 수 있는 형식인지 (DB 일치 확인은 호출하는 쪽에서)
    public boolean isRefreshTokenType(String token) {
        Object type;
        try {
            type = getDataFrom(token).get(TYPE_CLAIM);
        } catch (ExpiredJwtException e) {
            // 만료 여부는 지금처럼 DB 일치로만 판단
            type = e.getClaims().get(TYPE_CLAIM);
        } catch (Exception e) {
            type = null;
        }
        if (type == null) {
            return ALLOW_UNTYPED_LEGACY_REFRESH;
        }
        return TYPE_REFRESH.equals(type);
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 토큰에서 데이터 추출
    public Map<String, Object> getDataFrom(String token) {
        Jws<Claims> claims = Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);

        return claims.getBody();
    }

    // 리프레시 토큰 - 수정
    public String genRefreshToken() {
        return UUID.randomUUID().toString();
    }
}