package com.ll.demo.domain.group.group.service;

import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.group.group.dto.GroupInviteResponse;
import com.ll.demo.domain.group.group.dto.GroupRequest;
import com.ll.demo.domain.group.group.dto.GroupResponse;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.entity.InviteType;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.group.group.entity.JoinStatus;
import com.ll.demo.domain.group.group.entity.GroupJoinRequest;
import com.ll.demo.domain.group.group.dto.GroupDetailResponse;
import com.ll.demo.domain.group.group.dto.GroupJoinRequestResponse;
import com.ll.demo.domain.group.group.repository.GroupJoinRequestRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.global.exceptions.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MemberRepository memberRepository;
    private final FriendshipRepository friendshipRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final QuoteRepository quoteRepository;
    private final NotificationService notificationService;

    private static final int MAX_GROUP_MEMBERS = 5;
    private static final String CAPACITY_ERROR = "최대 인원 5명이 가득 차 들어갈 수 없어요";

    private void validateGroupCapacity(Group group) {
        if (groupMemberRepository.countByGroup(group) >= MAX_GROUP_MEMBERS) {
            throw new GlobalException("400-1", CAPACITY_ERROR);
        }
    }

    // 그룹 생성
    public GroupResponse createGroup(Member leader, GroupRequest req) {
        Group group = Group.builder()
                .name(req.name())
                .motto(req.motto())
                .leader(leader)
                .build();
        groupRepository.save(group);

        if (!groupMemberRepository.existsByGroupAndMember(group, leader)) {
            groupMemberRepository.save(GroupMember.builder()
                    .group(group)
                    .member(leader)
                    .build());
        }

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getMotto(),
                leader.getNickname(),
                1
        );
    }

    // 내가 가입한 그룹 조회
    @Transactional(readOnly = true)
    public List<GroupResponse> getMyGroups(Member member) {
        return groupMemberRepository.findByMember(member).stream()
                .map(GroupMember::getGroup)
                .filter(Objects::nonNull)
                .map(group -> {
                    try {
                        long memberCount = groupMemberRepository.countByGroup(group);
                        return new GroupResponse(
                                group.getId(),
                                group.getName(),
                                group.getMotto(),
                                group.getLeader().getNickname(),
                                memberCount
                        );
                    } catch (EntityNotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // 그룹 초대
    @Transactional
    public void inviteFriend(Member requester, Long groupId, Long friendId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GlobalException("404", "그룹을 찾을 수 없습니다."));

        if (!group.getLeader().getId().equals(requester.getId())) {
            throw new GlobalException("403", "권한이 없습니다.");
        }

        Member friend = memberRepository.findById(friendId)
                .orElseThrow(() -> new GlobalException("404", "초대할 회원을 찾을 수 없습니다."));

        if (!friendshipRepository.existsByMemberAndFriend(requester, friend)) {
            throw new GlobalException("400-2", "친구 관계인 회원만 초대할 수 있습니다.");
        }

        if (groupMemberRepository.countByGroup(group) >= MAX_GROUP_MEMBERS) {
            throw new GlobalException("400-1", CAPACITY_ERROR);
        }

        if (groupMemberRepository.existsByGroupAndMember(group, friend)) {
            throw new GlobalException("400-3", "이미 이 그룹의 멤버입니다.");
        }

        boolean alreadyInvited = groupJoinRequestRepository
                .existsByGroupAndRequesterAndStatusAndType(group, friend, JoinStatus.PENDING, InviteType.INVITE);
        if (alreadyInvited) {
            throw new GlobalException("400-4", "이미 초대 대기 중입니다.");
        }

        groupJoinRequestRepository.save(GroupJoinRequest.builder()
                .group(group)
                .inviter(requester)
                .requester(friend)
                .status(JoinStatus.PENDING)
                .type(InviteType.INVITE)
                .build());

        // 초대 알림
        notificationService.create(
                friend,
                requester,
                "GROUP_INVITE",
                requester.getNickname() + "님이 '" + group.getName() + "' 그룹에 초대했습니다.",
                group.getId()
        );
    }

    // 내 초대 목록 조회
    @Transactional(readOnly = true)
    public List<GroupInviteResponse> getMyInvitations(Member member) {
        return groupJoinRequestRepository
                .findAllByRequesterAndStatusAndType(member, JoinStatus.PENDING, InviteType.INVITE)
                .stream()
                .map(GroupInviteResponse::from)
                .toList();
    }

    // 특정 그룹에서 내가 보낸 초대 대기 목록 조회 (그룹장 전용)
    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponse> getSentInvitations(Member leader, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GlobalException("404", "그룹을 찾을 수 없습니다."));

        if (!group.getLeader().getId().equals(leader.getId())) {
            throw new GlobalException("403", "권한이 없습니다.");
        }

        return groupJoinRequestRepository
                .findAllByGroupAndInviterAndStatusAndType(group, leader, JoinStatus.PENDING, InviteType.INVITE)
                .stream()
                .map(GroupJoinRequestResponse::from)
                .toList();
    }

    // 특정 그룹에서 보낸 초대 취소 (그룹장 전용)
    @Transactional
    public void cancelInvitation(Member leader, Long groupId, Long requestId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GlobalException("404", "그룹을 찾을 수 없습니다."));

        if (!group.getLeader().getId().equals(leader.getId())) {
            throw new GlobalException("403", "권한이 없습니다.");
        }

        GroupJoinRequest req = groupJoinRequestRepository
                .findByIdAndGroupAndInviterAndStatusAndType(requestId, group, leader, JoinStatus.PENDING, InviteType.INVITE)
                .orElseThrow(() -> new GlobalException("404", "초대를 찾을 수 없습니다."));

        if (!req.getGroup().getId().equals(groupId)) {
            throw new GlobalException("403", "권한이 없습니다.");
        }

        req.reject();

        notificationService.create(
                req.getRequester(),
                leader,
                "GROUP_REJECT",
                leader.getNickname() + "님이 '" + group.getName() + "' 그룹 초대를 취소했습니다.",
                group.getId()
        );
    }

    // 그룹 초대 수락
    @Transactional
    public void acceptInvitation(Member member, Long requestId) {
        GroupJoinRequest req = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new GlobalException("404", "초대를 찾을 수 없습니다."));

        if (!req.getRequester().getId().equals(member.getId())) {
            throw new GlobalException("403", "권한이 없습니다.");
        }
        if (req.getStatus() != JoinStatus.PENDING || req.getType() != InviteType.INVITE) {
            throw new GlobalException("400-5", "처리할 수 없는 초대입니다.");
        }

        Group group = req.getGroup();
        if (groupMemberRepository.countByGroup(group) >= MAX_GROUP_MEMBERS) {
            throw new GlobalException("400-1", CAPACITY_ERROR);
        }
        if (!groupMemberRepository.existsByGroupAndMember(group, member)) {
            groupMemberRepository.save(GroupMember.builder().group(group).member(member).build());
        }
        req.accept();

        // 그룹 리더에게 수락 알림
        notificationService.create(
                group.getLeader(),
                member,
                "GROUP_ACCEPT",
                member.getNickname() + "님이 '" + group.getName() + "' 그룹 초대를 수락했습니다.",
                group.getId()
        );
    }

    // 그룹 초대 거절
    @Transactional
    public void rejectInvitation(Member member, Long requestId) {
        GroupJoinRequest req = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new GlobalException("404", "초대를 찾을 수 없습니다."));

        if (!req.getRequester().getId().equals(member.getId())) {
            throw new GlobalException("403", "권한이 없습니다.");
        }
        if (req.getStatus() != JoinStatus.PENDING || req.getType() != InviteType.INVITE) {
            throw new GlobalException("400-5", "처리할 수 없는 초대입니다.");
        }

        Group group = req.getGroup();
        req.reject();

        // 그룹 리더에게 거절 알림
        notificationService.create(
                group.getLeader(),
                member,
                "GROUP_REJECT",
                member.getNickname() + "님이 '" + group.getName() + "' 그룹 초대를 거절했습니다.",
                group.getId()
        );
    }

    // 탈퇴 / 강퇴
    public void removeOrLeaveMember(Member requester, Long groupId, Long targetId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GlobalException("404", "그룹을 찾을 수 없습니다."));
        Member target = memberRepository.findById(targetId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));
        GroupMember targetGM = groupMemberRepository.findByGroupAndMember(group, target)
                .orElseThrow(() -> new GlobalException("404", "그룹 멤버가 아닙니다."));

        if (group.getLeader().getId().equals(requester.getId()) || requester.getId().equals(targetId)) {
            if (group.getLeader().getId().equals(targetId)) {
                throw new GlobalException("400-6", "리더는 탈퇴할 수 없습니다.");
            }
            groupMemberRepository.delete(targetGM);

            // 탈퇴/강퇴 알림: 탈퇴 본인이면 리더에게, 강퇴면 대상에게
            boolean isSelfLeave = requester.getId().equals(targetId);
            if (isSelfLeave) {
                // 본인 탈퇴 → 리더에게 알림
                notificationService.create(
                        group.getLeader(),
                        target,
                        "GROUP_LEAVE",
                        target.getNickname() + "님이 '" + group.getName() + "' 그룹을 탈퇴했습니다.",
                        group.getId()
                );
            } else {
                // 리더가 강퇴 → 대상에게 알림
                notificationService.create(
                        target,
                        requester,
                        "GROUP_LEAVE",
                        "'" + group.getName() + "' 그룹에서 내보내졌습니다.",
                        group.getId()
                );
            }
        } else {
            throw new GlobalException("403", "권한이 없습니다.");
        }
    }

    // 그룹 가입 요청
    public void requestToJoin(Member user, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GlobalException("404", "그룹을 찾을 수 없습니다."));

        validateGroupCapacity(group);

        if (groupMemberRepository.existsByGroupAndMember(group, user)) {
            throw new GlobalException("400-3", "이미 그룹 멤버입니다.");
        }
        boolean alreadyRequested = groupJoinRequestRepository.existsByGroupAndRequesterAndStatus(
                group, user, JoinStatus.PENDING
        );
        if (alreadyRequested) {
            throw new GlobalException("400-4", "이미 가입 승인 대기 중입니다.");
        }
        groupJoinRequestRepository.save(GroupJoinRequest.builder()
                .group(group).requester(user).status(JoinStatus.PENDING).build());
    }

    // 가입 요청 승인
    public void acceptJoinRequest(Member leader, Long requestId) {
        GroupJoinRequest joinReq = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new GlobalException("404", "요청을 찾을 수 없습니다."));
        Group group = joinReq.getGroup();

        if (!group.getLeader().getId().equals(leader.getId())) {
            throw new GlobalException("403", "권한이 없습니다.");
        }

        if (!groupMemberRepository.existsByGroupAndMember(group, joinReq.getRequester())) {
            validateGroupCapacity(group);
            groupMemberRepository.save(GroupMember.builder().group(group).member(joinReq.getRequester()).build());
        }
        joinReq.accept();

        // 요청자에게 수락 알림
        notificationService.create(
                joinReq.getRequester(),
                leader,
                "GROUP_ACCEPT",
                "'" + group.getName() + "' 그룹 가입 요청이 승인되었습니다.",
                group.getId()
        );
    }

    // 가입 요청 거절
    public void rejectJoinRequest(Member leader, Long requestId) {
        GroupJoinRequest joinReq = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new GlobalException("404", "요청을 찾을 수 없습니다."));
        Group group = joinReq.getGroup();

        if (!group.getLeader().getId().equals(leader.getId())) {
            throw new GlobalException("403", "권한이 없습니다.");
        }
        if (joinReq.getStatus() != JoinStatus.PENDING) {
            throw new GlobalException("400", "처리할 수 없는 요청입니다.");
        }

        joinReq.reject();

        // 요청자에게 거절 알림
        notificationService.create(
                joinReq.getRequester(),
                leader,
                "GROUP_REJECT",
                "'" + group.getName() + "' 그룹 가입 요청이 거절되었습니다.",
                group.getId()
        );
    }

    // 그룹 가입 요청 목록 조회 (그룹장 전용)
    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponse> getJoinRequests(Member leader, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GlobalException("404", "그룹을 찾을 수 없습니다."));

        if (!group.getLeader().getId().equals(leader.getId())) {
            throw new GlobalException("403", "권한이 없습니다.");
        }

        return groupJoinRequestRepository
                .findAllByGroupAndStatusAndType(group, JoinStatus.PENDING, InviteType.JOIN_REQUEST)
                .stream()
                .map(GroupJoinRequestResponse::from)
                .toList();
    }

    // 그룹 상세 조회
    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupDetail(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GlobalException("404", "그룹을 찾을 수 없습니다."));

        List<GroupMember> groupMembers = groupMemberRepository.findByGroup(group);
        List<Member> members = groupMembers.stream().map(GroupMember::getMember).toList();

        long totalQuoteCount = quoteRepository.countByAuthorIn(members);

        List<GroupDetailResponse.MemberInfo> memberInfos = members.stream()
                .map(m -> new GroupDetailResponse.MemberInfo(
                        m.getId(),
                        m.getNickname(),
                        m.getProfileImage(),
                        m.getIntroduction()
                ))
                .collect(Collectors.toList());

        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getMotto(),
                group.getLeader().getNickname(),
                groupMembers.size(),
                totalQuoteCount,
                group.getCreateDate(),
                memberInfos
        );
    }

    // 그룹 좌우명 수정
    public void updateMotto(Member requester, Long groupId, String newMotto) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GlobalException("404", "그룹을 찾을 수 없습니다."));

        if (!groupMemberRepository.existsByGroupAndMember(group, requester)) {
            throw new GlobalException("403", "그룹 멤버만 좌우명을 수정할 수 있습니다.");
        }
        group.setMotto(newMotto);

        // 그룹원 전체에게 메시지 변경 알림 발송
        groupMemberRepository.findByGroup(group).stream()
                .map(GroupMember::getMember)
                .filter(m -> !m.getId().equals(requester.getId()))
                .forEach(member -> notificationService.create(
                        member,
                        requester,
                        "GROUP_MOTTO",
                        requester.getNickname() + "님이 '" + group.getName() + "' 그룹 좌우명을 변경했습니다.",
                        group.getId()
                ));
    }

    // 그룹 삭제
    @Transactional
    public void deleteGroup(Member actor, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GlobalException("404", "해당 그룹을 찾을 수 없습니다."));

        if (!group.getLeader().getId().equals(actor.getId())) {
            throw new GlobalException("403", "그룹 삭제 권한이 없습니다.");
        }

        groupRepository.delete(group);
    }
}
