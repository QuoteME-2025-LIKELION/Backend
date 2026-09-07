package com.ll.demo.domain.group.group.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.group.group.dto.GroupJoinRequestResponse;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupJoinRequest;
import com.ll.demo.domain.group.group.entity.InviteType;
import com.ll.demo.domain.group.group.entity.JoinStatus;
import com.ll.demo.domain.group.group.repository.GroupJoinRequestRepository;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.exceptions.GlobalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GroupServiceTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupJoinRequestRepository groupJoinRequestRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    private Member leader;
    private Member invitee1;
    private Member invitee2;
    private Member otherLeader;
    private Member normalMember;
    private Group group;
    private Group otherGroup;

    @BeforeEach
    void setUp() {
        leader = memberRepository.save(Member.builder()
                .email("leader@test.com")
                .nickname("리더")
                .password("password123")
                .build());

        invitee1 = memberRepository.save(Member.builder()
                .email("invitee1@test.com")
                .nickname("초대받은1")
                .password("password123")
                .build());

        invitee2 = memberRepository.save(Member.builder()
                .email("invitee2@test.com")
                .nickname("초대받은2")
                .password("password123")
                .build());

        otherLeader = memberRepository.save(Member.builder()
                .email("otherLeader@test.com")
                .nickname("다른리더")
                .password("password123")
                .build());

        normalMember = memberRepository.save(Member.builder()
                .email("member@test.com")
                .nickname("일반멤버")
                .password("password123")
                .build());

        group = groupRepository.save(Group.builder()
                .name("테스트그룹")
                .motto("테스트")
                .leader(leader)
                .build());

        otherGroup = groupRepository.save(Group.builder()
                .name("다른그룹")
                .motto("다른")
                .leader(otherLeader)
                .build());

        groupMemberRepository.saveAll(List.of(
                com.ll.demo.domain.group.group.entity.GroupMember.builder()
                        .group(group)
                        .member(leader)
                        .build(),
                com.ll.demo.domain.group.group.entity.GroupMember.builder()
                        .group(group)
                        .member(normalMember)
                        .build()
        ));

        groupMemberRepository.save(com.ll.demo.domain.group.group.entity.GroupMember.builder()
                .group(otherGroup)
                .member(otherLeader)
                .build());
    }

    @Test
    void inviteFriend_setsInviterOnNewInvite() {
        friendshipRepository.save(Friendship.builder()
                .member(leader)
                .friend(invitee1)
                .status(FriendshipStatus.ACCEPTED)
                .build());

        groupService.inviteFriend(leader, group.getId(), invitee1.getId());

        GroupJoinRequest saved = groupJoinRequestRepository.findAll().stream()
                .filter(req -> req.getType() == InviteType.INVITE && req.getRequester().getId().equals(invitee1.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(saved.getInviter()).isNotNull();
        assertThat(saved.getInviter().getId()).isEqualTo(leader.getId());
    }

    @Test
    void getSentInvitations_returnsOnlyPendingForCurrentLeader() {
        GroupJoinRequest pending1 = groupJoinRequestRepository.save(
                GroupJoinRequest.builder().group(group).inviter(leader).requester(invitee1).status(JoinStatus.PENDING).type(InviteType.INVITE).build());
        GroupJoinRequest pending2 = groupJoinRequestRepository.save(
                GroupJoinRequest.builder().group(group).inviter(leader).requester(invitee2).status(JoinStatus.PENDING).type(InviteType.INVITE).build());
        groupJoinRequestRepository.save(
                GroupJoinRequest.builder().group(group).inviter(leader).requester(invitee1).status(JoinStatus.ACCEPTED).type(InviteType.INVITE).build());
        groupJoinRequestRepository.save(
                GroupJoinRequest.builder().group(otherGroup).inviter(otherLeader).requester(invitee1).status(JoinStatus.PENDING).type(InviteType.INVITE).build());

        List<GroupJoinRequestResponse> result = groupService.getSentInvitations(leader, group.getId());

        assertThat(result).extracting(GroupJoinRequestResponse::requestId)
                .containsExactlyInAnyOrder(pending1.getId(), pending2.getId());
    }

    @Test
    void getSentInvitations_requiresLeaderAndValidGroup() {
        assertThrows(GlobalException.class, () -> groupService.getSentInvitations(normalMember, group.getId()));
        assertThrows(GlobalException.class, () -> groupService.getSentInvitations(leader, 99999L));
    }

    @Test
    void cancelInvitation_allowsLeaderToCancelPendingOnly() {
        GroupJoinRequest pending = groupJoinRequestRepository.save(
                GroupJoinRequest.builder().group(group).inviter(leader).requester(invitee1).status(JoinStatus.PENDING).type(InviteType.INVITE).build());
        GroupJoinRequest accepted = groupJoinRequestRepository.save(
                GroupJoinRequest.builder().group(group).inviter(leader).requester(invitee2).status(JoinStatus.ACCEPTED).type(InviteType.INVITE).build());
        GroupJoinRequest rejected = groupJoinRequestRepository.save(
                GroupJoinRequest.builder().group(group).inviter(leader).requester(invitee1).status(JoinStatus.REJECTED).type(InviteType.INVITE).build());
        GroupJoinRequest otherGroupInvite = groupJoinRequestRepository.save(
                GroupJoinRequest.builder().group(otherGroup).inviter(otherLeader).requester(invitee1).status(JoinStatus.PENDING).type(InviteType.INVITE).build());

        groupService.cancelInvitation(leader, group.getId(), pending.getId());
        assertThat(groupJoinRequestRepository.findById(pending.getId())).isPresent();
        assertThat(groupJoinRequestRepository.findById(pending.getId()).get().getStatus()).isEqualTo(JoinStatus.REJECTED);

        assertThrows(GlobalException.class, () -> groupService.cancelInvitation(leader, group.getId(), accepted.getId()));
        assertThrows(GlobalException.class, () -> groupService.cancelInvitation(leader, group.getId(), rejected.getId()));
        assertThrows(GlobalException.class, () -> groupService.cancelInvitation(otherLeader, group.getId(), pending.getId()));
        assertThrows(GlobalException.class, () -> groupService.cancelInvitation(leader, otherGroup.getId(), otherGroupInvite.getId()));
        assertThrows(GlobalException.class, () -> groupService.cancelInvitation(normalMember, group.getId(), pending.getId()));
        assertThrows(GlobalException.class, () -> groupService.cancelInvitation(leader, group.getId(), 99999L));
    }

    @Test
    void legacyInviteWithoutInviter_remainsNullableAndDoesNotAutoBackfill() {
        GroupJoinRequest legacy = groupJoinRequestRepository.save(
                GroupJoinRequest.builder().group(group).requester(invitee2).status(JoinStatus.PENDING).type(InviteType.INVITE).build());

        assertThat(legacy.getInviter()).isNull();

        assertThat(groupService.getSentInvitations(leader, group.getId()))
                .extracting(GroupJoinRequestResponse::requestId)
                .doesNotContain(legacy.getId());

        GroupJoinRequest persisted = groupJoinRequestRepository.findById(legacy.getId()).orElseThrow();
        assertThat(persisted.getInviter()).isNull();
    }
}

