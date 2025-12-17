package com.ll.demo.domain.group.group.repository;

import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupJoinRequest;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.group.group.entity.JoinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    // 사용자, 요청 중복 확인
    Optional<GroupJoinRequest> findByGroupAndRequesterAndStatus(Group group, Member requester, JoinStatus status);
    // 여부만 확인
    boolean existsByGroupAndRequesterAndStatus(Group group, Member requester, JoinStatus status);
}