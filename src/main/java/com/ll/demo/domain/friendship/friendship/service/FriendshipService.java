package com.ll.demo.domain.friendship.friendship.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void addFriendship(Member memberA, Long memberBId) {

        Member memberB = memberRepository.findById(memberBId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 회원을 찾을 수 없습니다."));

        if (memberA.getId().equals(memberBId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 친구로 추가할 수 없습니다.");
        }

        if (friendshipRepository.existsByMemberAndFriend(memberA, memberB)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 친구 관계입니다.");
        }

        Friendship friendshipAtoB = Friendship.builder()
                .member(memberA)
                .friend(memberB)
                .status(FriendshipStatus.ACCEPTED) // 즉시 ACCEPTED
                .build();
        friendshipRepository.save(friendshipAtoB);

        Friendship friendshipBtoA = Friendship.builder()
                .member(memberB)
                .friend(memberA)
                .status(FriendshipStatus.ACCEPTED) // 즉시 ACCEPTED
                .build();
        friendshipRepository.save(friendshipBtoA);
    }

    @Transactional
    public void removeFriend(Member actor, Long friendId) {
        Member friend = memberRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));
        // 쌍방향 삭제
        friendshipRepository.deleteByMemberAndFriend(actor, friend);
        friendshipRepository.deleteByMemberAndFriend(friend, actor);
    }
}