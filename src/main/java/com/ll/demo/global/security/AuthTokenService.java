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

@Service
@RequiredArgsConstructor
public class AuthTokenService {
    @Value("${custom.secret.jwt.secretKey}")
    private String secretKey;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // Access Token 생성
    public String genToken(Member member, int expirationSec) {
        Date expiration = Date.from(Instant.now().plusSeconds(expirationSec));

        return Jwts.builder()
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .setExpiration(expiration)
                .claim("id", member.getId())
                .compact();
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
}