package com.ll.demo.domain.notification.notification.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.entity.NotificationSetting;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PokeNotificationSettingTest {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationSettingRepository notificationSettingRepository;
    @Autowired private MemberRepository memberRepository;

    @ParameterizedTest
    @CsvSource({"true,true,1", "true,false,0", "false,true,0", "false,false,0"})
    @DisplayName("콕 알림은 friendEnabled와 pokeEnabled가 모두 켜졌을 때만 생성")
    void pokeRequiresBothSettings(boolean friendEnabled, boolean pokeEnabled, long expected) {
        Member sender = memberRepository.save(Member.builder().email("s@test.com").nickname("보냄").password("password123").build());
        Member receiver = memberRepository.save(Member.builder().email("r@test.com").nickname("받음").password("password123").build());
        notificationSettingRepository.save(NotificationSetting.builder()
                .member(receiver).friendEnabled(friendEnabled).pokeEnabled(pokeEnabled).build());
        long before = notificationRepository.count();

        notificationService.create(receiver, sender, "POKE", "콕", sender.getId());

        assertThat(notificationRepository.count() - before).isEqualTo(expected);
    }
}
