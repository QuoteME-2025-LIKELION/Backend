package com.ll.demo.domain.group.group.repository;

import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    long countByGroup(Group group);
    boolean existsByGroupAndMember(Group group, Member member);
    Optional<GroupMember> findByGroupAndMember(Group group, Member member);
    List<GroupMember> findByMember(Member member); // 내가 가입한 그룹 조회
    List<GroupMember> findByGroup(Group group); // 그룹 찾기
}