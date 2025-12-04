package com.ll.demo.domain.member.member.repository;

import com.ll.demo.domain.member.member.entity.Friendship;
import com.ll.demo.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findAllByMember(Member member);
}