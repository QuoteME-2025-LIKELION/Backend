package com.ll.demo.domain.push.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.push.entity.DevicePlatform;
import com.ll.demo.domain.push.entity.DeviceToken;
import com.ll.demo.domain.push.repository.DeviceTokenRepository;
import com.ll.demo.global.exceptions.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Transactional
public class PushTokenService {
    // 공유 게스트 계정에 토큰을 받으면 다른 사람에게 푸시가 간다
    private static final String GUEST_EMAIL = "guest@guest.com";

    private final DeviceTokenRepository deviceTokenRepository;

    // 같은 토큰이 있으면 소유자를 현재 회원으로 옮기고 lastSeenAt 갱신
    public void register(Member member, String token, DevicePlatform platform) {
        if (GUEST_EMAIL.equals(member.getEmail())) {
            throw new GlobalException("403", "게스트 계정은 푸시 알림을 받을 수 없습니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        deviceTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> existing.reassign(member, platform, now),
                () -> deviceTokenRepository.save(DeviceToken.builder()
                        .member(member).token(token).platform(platform).lastSeenAt(now).build())
        );
    }

    // 본인 토큰만 삭제 (로그아웃 시 호출, 없으면 아무 일도 하지 않음)
    public void unregister(Member member, String token) {
        deviceTokenRepository.deleteByTokenAndMember(token, member);
    }

    // FCM이 무효라고 응답한 토큰 정리
    public void deleteInvalidTokens(Collection<String> tokens) {
        if (!tokens.isEmpty()) {
            deviceTokenRepository.deleteAllByTokenIn(tokens);
        }
    }
}
