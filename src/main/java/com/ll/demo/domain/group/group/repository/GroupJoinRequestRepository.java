package com.ll.demo.domain.group.group.repository;

import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupJoinRequest;
import com.ll.demo.domain.group.group.entity.InviteType;
import com.ll.demo.domain.group.group.entity.JoinStatus;
import com.ll.demo.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    // 사용자, 요청 중복 확인
    Optional<GroupJoinRequest> findByGroupAndRequesterAndStatus(Group group, Member requester, JoinStatus status);
    // 여부만 확인
    boolean existsByGroupAndRequesterAndStatus(Group group, Member requester, JoinStatus status);

    // 타입 구분 중복 확인
    boolean existsByGroupAndRequesterAndStatusAndType(Group group, Member requester, JoinStatus status, InviteType type);

    // 내게 온 대기 중인 초대 목록
    List<GroupJoinRequest> findAllByRequesterAndStatusAndType(Member requester, JoinStatus status, InviteType type);

    // 그룹의 대기 중인 가입 요청 목록 (리더 확인용)
    List<GroupJoinRequest> findAllByGroupAndStatusAndType(Group group, JoinStatus status, InviteType type);

    // 특정 그룹에서 특정 그룹장이 보낸 대기 중인 초대 목록
    List<GroupJoinRequest> findAllByGroupAndInviterAndStatusAndType(Group group, Member inviter, JoinStatus status, InviteType type);

    // 특정 초대의 groupId + inviter + status + type 검증
    Optional<GroupJoinRequest> findByIdAndGroupAndInviterAndStatusAndType(Long id, Group group, Member inviter, JoinStatus status, InviteType type);
}