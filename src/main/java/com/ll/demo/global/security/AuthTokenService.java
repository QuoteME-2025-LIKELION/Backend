package com.ll.demo.global.security;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ll.demo.domain.member.member.entity.Member;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthTokenService {

    // 나중에 secret key, jwt토큰 발행 및 로직 구현

    // 임시
    public boolean validateToken(String token) {
        log.info("토큰 검증 요청: {}", token);
        // 실제 로직
        return true;
    }

    // 토큰 생성
    public String genToken(Member member, int expirationSec) {
        //
        return "";
    }

    // 토큰에서 데이터 추출
    public java.util.Map<String, Object> getDataFrom(String token) {
        //
        return new java.util.HashMap<>();
    }
}