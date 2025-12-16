package com.ll.demo.domain.friendship.friendship.repository;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findAllByMember(Member member);
    // 중복 요청 방지
    boolean existsByMemberAndFriend(Member member, Member friend);
    // 받은 친구 요청 목록 조회
    List<Friendship> findByFriendAndStatus(Member friend, FriendshipStatus status);
    //친구 수
    long countByMember(Member member);
    // 친구삭제
    void deleteByMemberAndFriend(Member member, Member friend);
}