package com.ll.demo.domain.push;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.notification.entity.NotificationSetting;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.notification.service.QuoteReminderSender;
import com.ll.demo.domain.push.entity.DevicePlatform;
import com.ll.demo.domain.push.repository.DeviceTokenRepository;
import com.ll.demo.domain.push.service.PushTokenService;
import com.ll.demo.global.push.PushMessage;
import com.ll.demo.global.push.PushSender;
import com.ll.demo.global.security.AuthTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 커밋이 일어나야 AFTER_COMMIT 리스너가 동작하므로 테스트 트랜잭션을 쓰지 않고 직접 정리한다
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PushNotificationTest {

    @MockBean private PushSender pushSender;

    @Autowired private NotificationService notificationService;
    @Autowired private QuoteReminderSender quoteReminderSender;
    @Autowired private PushTokenService pushTokenService;
    @Autowired private DeviceTokenRepository deviceTokenRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationSettingRepository notificationSettingRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MockMvc mvc;

    private TransactionTemplate tx;
    private Member receiver;
    private Member sender;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        receiver = memberRepository.save(Member.builder().email("push-receiver@test.com").nickname("수신자").password("password123").build());
        sender = memberRepository.save(Member.builder().email("push-sender@test.com").nickname("발신자").password("password123").build());
        pushTokenService.register(receiver, "receiver-web-token", DevicePlatform.WEB);
        pushTokenService.register(receiver, "receiver-android-token", DevicePlatform.ANDROID);
        given(pushSender.send(anyList(), any())).willReturn(List.of());
    }

    @AfterEach
    void cleanUp() {
        tx.executeWithoutResult(status -> {
            for (Member member : List.of(receiver, sender)) {
                Member m = memberRepository.findById(member.getId()).orElseThrow();
                notificationRepository.deleteAllByMember(m);
                friendshipRepository.deleteAllByMemberOrFriend(m);
                notificationSettingRepository.deleteByMember(m);
                deviceTokenRepository.deleteAllByMember(m);
                memberRepository.delete(m);
            }
        });
    }

    private void waitUntil(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 50 && !condition.getAsBoolean(); i++) {
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private PushMessage capturedMessage() {
        ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushSender, timeout(5000)).send(anyList(), message.capture());
        return message.getValue();
    }

    @Test
    @DisplayName("알림 1건 생성 → 커밋 후 토큰 2개로 1회 발송, 제목 QuoteME·본문은 인앱 문구")
    @SuppressWarnings("unchecked")
    void sendsOnceToAllTokensAfterCommit() {
        notificationService.create(receiver, sender, "FRIEND_REQUEST", "발신자님이 친구 요청을 보냈습니다.", sender.getId());

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushSender, timeout(5000).times(1)).send(tokens.capture(), message.capture());

        assertThat(tokens.getValue()).containsExactlyInAnyOrder("receiver-web-token", "receiver-android-token");
        assertThat(message.getValue().title()).isEqualTo("QuoteME");
        assertThat(message.getValue().body()).isEqualTo("발신자님이 친구 요청을 보냈습니다.");
        Long notificationId = notificationRepository.findByReceiverIdOrderByCreateDateDesc(receiver.getId()).get(0).getId();
        assertThat(message.getValue().data()).containsEntry("notificationId", String.valueOf(notificationId))
                .containsEntry("type", "FRIEND_REQUEST")
                .containsEntry("targetId", String.valueOf(sender.getId()));
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 발송하지 않음")
    void rolledBack_notSent() {
        tx.executeWithoutResult(status -> {
            notificationService.create(receiver, sender, "FRIEND_REQUEST", "롤백될 알림", sender.getId());
            status.setRollbackOnly();
        });

        verify(pushSender, after(500).never()).send(anyList(), any());
    }

    @Test
    @DisplayName("카테고리가 꺼져 있으면 인앱 알림도 푸시도 없음")
    void categoryOff_notSent() {
        notificationSettingRepository.save(NotificationSetting.builder().member(receiver).friendEnabled(false).build());

        notificationService.create(receiver, sender, "FRIEND_REQUEST", "꺼진 카테고리", sender.getId());

        verify(pushSender, after(500).never()).send(anyList(), any());
        assertThat(notificationRepository.findByReceiverIdOrderByCreateDateDesc(receiver.getId())).isEmpty();
    }

    @Test
    @DisplayName("발송 예외 → 원래 요청은 성공, 인앱 알림 유지")
    void sendFailure_doesNotAffectRequest() throws Exception {
        given(pushSender.send(anyList(), any())).willThrow(new IllegalStateException("fcm down"));

        mvc.perform(post("/api/friends/request/" + receiver.getId())
                        .header("Authorization", "Bearer " + authTokenService.genToken(sender, 600)))
                .andExpect(status().isCreated());

        verify(pushSender, timeout(5000)).send(anyList(), any());
        assertThat(notificationRepository.findByReceiverIdAndTypeOrderByCreateDateDesc(receiver.getId(), "FRIEND_REQUEST")).hasSize(1);
    }

    @Test
    @DisplayName("FCM이 무효라고 응답한 토큰은 삭제")
    void invalidToken_deleted() throws InterruptedException {
        given(pushSender.send(anyList(), any())).willReturn(List.of("receiver-web-token"));

        notificationService.create(receiver, sender, "POKE", "콕!", sender.getId());

        waitUntil(() -> deviceTokenRepository.findByToken("receiver-web-token").isEmpty());
        assertThat(deviceTokenRepository.findTokensByMemberId(receiver.getId())).containsExactly("receiver-android-token");
    }

    @Test
    @DisplayName("리마인더 알림도 푸시로 발송")
    void reminder_sent() {
        quoteReminderSender.sendForMemberWithoutSetting(receiver.getId(), LocalDate.now());

        PushMessage message = capturedMessage();
        assertThat(message.body()).isEqualTo("오늘의 명언을 작성할 시간이에요!");
        assertThat(message.data()).containsEntry("type", QuoteReminderSender.TYPE).containsEntry("targetId", "");
    }

    @Test
    @DisplayName("익명 알림(그룹 메시지 변경) data에 발신자 정보가 없음")
    void anonymousNotification_hasNoSenderData() {
        notificationService.create(receiver, sender, "GROUP_MOTTO", "'우리' 그룹 메시지가 변경되었어요", 99L);

        PushMessage message = capturedMessage();
        assertThat(message.data()).containsOnlyKeys("notificationId", "type", "targetId");
        assertThat(message.data().values()).doesNotContain(String.valueOf(sender.getId()), sender.getNickname(), sender.getEmail());
        assertThat(message.body()).doesNotContain(sender.getNickname());
    }
}
