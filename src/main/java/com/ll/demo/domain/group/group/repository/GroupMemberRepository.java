package com.ll.demo.domain.group.group.repository;

import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    long countByGroup(Group group);
    boolean existsByGroupAndMember(Group group, Member member);
    Optional<GroupMember> findByGroupAndMember(Group group, Member member);
    List<GroupMember> findByMember(Member member); // 내가 가입한 그룹 조회
    List<GroupMember> findByGroup(Group group); // 그룹 찾기
    // 그룹 이름 키워드가 포함된 그룹 멤버
    @Query("SELECT gm.member FROM GroupMember gm JOIN gm.group g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :groupNameKeyword, '%'))")
    List<Member> findMembersByGroupNameContaining(@Param("groupNameKeyword") String groupNameKeyword);
}