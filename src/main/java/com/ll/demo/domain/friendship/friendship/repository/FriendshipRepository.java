package com.ll.demo.domain.friendship.friendship.repository;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // 친구 요청 목록 - 변경
    List<Friendship> findAllByMemberAndStatus(Member member, FriendshipStatus status);

    // 엔티티 내부의 친구 ID
    @Query("SELECT f.friend.id FROM Friendship f WHERE f.member.id = :memberId AND f.status = com.ll.demo.domain.friendship.friendship.type.FriendshipStatus.ACCEPTED")
    List<Long> findFriendIds(@Param("memberId") Long memberId);

    // 중복 요청 방지
    boolean existsByMemberAndFriend(Member member, Member friend);
    boolean existsByMemberAndFriendAndStatus(Member member, Member friend, FriendshipStatus status);
    // 받은 친구 요청 목록 조회
    List<Friendship> findByFriendAndStatus(Member friend, FriendshipStatus status);
    // 친구 수
    long countByMemberAndStatus(Member member, FriendshipStatus status);
    // 친구삭제
    void deleteByMemberAndFriend(Member member, Member friend);

    Optional<Friendship> findByMemberAndFriend(Member member, Member friend);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE f.member = :member OR f.friend = :member")
    void deleteAllByMemberOrFriend(@Param("member") Member member);
}