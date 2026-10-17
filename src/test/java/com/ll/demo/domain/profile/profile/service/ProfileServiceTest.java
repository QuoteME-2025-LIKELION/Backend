package com.ll.demo.domain.profile.profile.service;

import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationSettingRepository notificationSettingRepository;

    @InjectMocks
    private ProfileService profileService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder().nickname("탈퇴자").build();
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    @Test
    @DisplayName("성공: 탈퇴 시 알림과 알림 설정을 먼저 삭제한 뒤 회원을 삭제한다")
    void deleteAccount_deletesNotificationsAndSettingBeforeMember() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        profileService.deleteAccount(member);

        InOrder inOrder = inOrder(notificationRepository, notificationSettingRepository, memberRepository);
        inOrder.verify(notificationRepository).deleteAllByMember(member);
        inOrder.verify(notificationSettingRepository).deleteByMember(member);
        inOrder.verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("실패: 회원이 없으면 예외가 발생하고 아무것도 삭제하지 않는다")
    void deleteAccount_memberNotFound_deletesNothing() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.deleteAccount(member))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(notificationRepository, notificationSettingRepository);
        verify(memberRepository, never()).delete(any());
    }
}
