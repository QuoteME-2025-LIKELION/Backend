package com.ll.demo.global.security.oauth2;

import com.ll.demo.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 앱 인증용 일회용 코드 (교환 코드·연동 티켓·연동 결과). 원문은 저장하지 않고 SHA-256 해시만 저장
@Entity
@Table(name = "oauth2_exchange_codes", indexes = @Index(name = "idx_oauth2_exchange_codes_expires_at", columnList = "expires_at"))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OAuth2ExchangeCode extends BaseEntity {

    public enum Purpose {
        LOGIN, LINK_TICKET, LINK_RESULT
    }

    @Column(name = "code_hash", nullable = false, unique = true, length = 64)
    private String codeHash;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "is_new_user", nullable = false)
    private boolean newUser;

    @Column(name = "code_challenge", nullable = false, length = 128)
    private String codeChallenge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // LINK_TICKET: 연동 후 돌아갈 앱 URI
    @Column(name = "redirect_uri", length = 512)
    private String redirectUri;

    // LINK_TICKET·LINK_RESULT: 연동 대상 소셜 서비스 (google, kakao)
    @Column(length = 20)
    private String provider;

    // LINK_RESULT: 인증된 소셜 계정 ID
    @Column(name = "provider_id")
    private String providerId;
}
