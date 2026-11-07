package com.ll.demo.domain.push.service;

import com.ll.demo.domain.push.event.NotificationCreatedEvent;
import com.ll.demo.domain.push.repository.DeviceTokenRepository;
import com.ll.demo.global.push.PushMessage;
import com.ll.demo.global.push.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

// 인앱 알림 커밋 후 푸시 발송 (실패해도 원래 요청·트랜잭션과 무관)
@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationListener {
    static final String TITLE = "QuoteME";

    private final DeviceTokenRepository deviceTokenRepository;
    private final PushTokenService pushTokenService;
    private final PushSender pushSender;

    @Async("pushExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        try {
            List<String> tokens = deviceTokenRepository.findTokensByMemberId(event.receiverId());
            if (tokens.isEmpty()) return;

            // 발신자·이메일 등은 넣지 않는다 (익명 알림 보호)
            Map<String, String> data = Map.of(
                    "notificationId", String.valueOf(event.notificationId()),
                    "type", event.type(),
                    "targetId", event.targetId() == null ? "" : String.valueOf(event.targetId())
            );
            List<String> invalidTokens = pushSender.send(tokens, new PushMessage(TITLE, event.message(), data));
            pushTokenService.deleteInvalidTokens(invalidTokens);
        } catch (Exception e) {
            log.warn("푸시 발송 실패 notificationId={}: {}", event.notificationId(), e.toString());
        }
    }
}
