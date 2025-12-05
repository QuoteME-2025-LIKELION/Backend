package com.ll.demo.domain.poke.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.poke.entity.Poke;
import com.ll.demo.domain.poke.repository.PokeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PokeService {

    private final PokeRepository pokeRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    //1. 콕 찌르기 (저장)
    @Transactional
    public boolean poke(Long senderId, Long receiverId) {
        // 자기 자신을 찌를 순 없음
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("자기 자신은 찌를 수 없습니다.");
        }

        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("보내는 회원을 찾을 수 없습니다."));

        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("받는 회원을 찾을 수 없습니다."));

        // (옵션) 하루에 한 번만 찌르기 가능하게 하려면 여기에 검증 로직 추가

        Poke poke = Poke.builder()
                .sender(sender)
                .receiver(receiver)
                .build();

        pokeRepository.save(poke);

        // 2. 알림 생성
        notificationService.create(
                receiver,
                sender,
                "POKE",
                sender.getName() + "님이 콕 찔렀어요! 👋",
                sender.getId()
        );
        return true;
    }

    // 나를 찌른 횟수 조회
    public long countMyPokes(Long myId) {
        return pokeRepository.countByReceiverId(myId);
    }
}