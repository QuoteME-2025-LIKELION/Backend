package com.ll.demo.domain.poke.poke.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.poke.repository.PokeRepository;
import com.ll.demo.domain.poke.service.PokeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PokeServiceTest {

    @Mock private PokeRepository pokeRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private PokeService pokeService;

    private Member sender;
    private Member receiver;

    @BeforeEach
    void setUp() {
        sender = Member.builder().nickname("보내는이").email("sender@test.com").build();
        ReflectionTestUtils.setField(sender, "id", 1L);

        receiver = Member.builder().nickname("받는이").email("receiver@test.com").build();
        ReflectionTestUtils.setField(receiver, "id", 2L);
    }

    @Test
    @DisplayName("성공: 타인을 콕 찌르면 데이터가 저장되고 알림이 생성된다")
    void poke_Success() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(receiver));

        // when
        boolean result = pokeService.poke(1L, 2L);

        // then
        assertThat(result).isTrue();
        verify(pokeRepository, times(1)).save(any());
        verify(notificationService, times(1)).create(
                eq(receiver), eq(sender), eq("POKE"), contains("찔렀어요"), eq(1L)
        );
    }

    @Test
    @DisplayName("실패: 자기 자신을 찌르려고 하면 예외가 발생한다")
    void poke_Fail_Self() {
        // when & then
        assertThatThrownBy(() -> pokeService.poke(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기 자신은 찌를 수 없습니다.");
    }

    @Test
    @DisplayName("실패: 존재하지 않는 회원을 찌르려고 하면 에러가 발생한다")
    void poke_Fail_MemberNotFound() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pokeService.poke(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("찾을 수 없습니다.");
    }

    @Test
    @DisplayName("성공: 콕 찌르기 시 DB 저장과 알림 발송이 동시에 일어나야 한다")
    void poke_Full_Success() {
        // 1. 준비 (Given)
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(receiver));

        // 2. 실행 (When)
        boolean result = pokeService.poke(1L, 2L);

        // 3. 검증 (Then)
        assertThat(result).isTrue();
        // 데이터 저장 확인
        verify(pokeRepository, times(1)).save(any());
        // 알림 생성 확인: 타입이 'POKE'이고 메시지에 보내는 사람 이름이 포함되었는가?
        verify(notificationService, times(1)).create(
                eq(receiver),
                eq(sender),
                eq("POKE"),
                argThat(msg -> msg.contains(sender.getName()) && msg.contains("콕 찔렀어요")),
                eq(1L)
        );
    }

    @Test
    @DisplayName("예외: 자기 자신을 찌를 경우 알림이 발송되지 않고 에러가 발생해야 한다")
    void poke_Self_Fail() {
        // 실행 & 검증
        assertThatThrownBy(() -> pokeService.poke(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        // 알림이나 저장이 절대 호출되면 안 됨
        verify(pokeRepository, never()).save(any());
        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }
}