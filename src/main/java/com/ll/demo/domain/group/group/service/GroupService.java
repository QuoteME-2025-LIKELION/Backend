package com.ll.demo.domain.group.group.service;

import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.group.group.dto.GroupRequest;
import com.ll.demo.domain.group.group.dto.GroupResponse;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.group.group.entity.JoinStatus;
import com.ll.demo.domain.group.group.entity.GroupJoinRequest;
import com.ll.demo.domain.group.group.dto.GroupDetailResponse;
import com.ll.demo.domain.group.group.repository.GroupJoinRequestRepository;
import com.ll.demo.domain.group.group.dto.MottoRequest;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

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

    public void createGroup(Member leader, GroupRequest req) {
        Group group = Group.builder().name(req.name()).motto(req.motto()).leader(leader).build();
        groupRepository.save(group);
        groupMemberRepository.save(GroupMember.builder().group(group).member(leader).build());
    }

    // 그룹 초대
    // 누구나 초대 가능하도록 수정
    public void inviteFriend(Member requester, Long groupId, Long friendId) { // 🟢 매개변수: friendId (Long)
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("그룹을 찾을 수 없습니다."));

        boolean isMember = groupMemberRepository.existsByGroupAndMember(group, requester);
        if (!isMember) throw new RuntimeException("그룹 멤버만 친구를 초대할 수 있습니다.");
        if (groupMemberRepository.countByGroup(group) >= 5) throw new RuntimeException("최대 인원(5명)을 초과했습니다.");
        Member friend = memberRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("초대할 회원을 찾을 수 없습니다."));
        if (!friendshipRepository.existsByMemberAndFriend(requester, friend)) throw new RuntimeException("친구 관계인 회원만 초대 가능합니다.");
        if (groupMemberRepository.existsByGroupAndMember(group, friend)) throw new RuntimeException("이미 그룹에 포함된 회원입니다.");

        GroupMember newMember = GroupMember.builder()
                .group(group)
                .member(friend)
                .build();
        groupMemberRepository.save(newMember);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getMyGroups(Member member) {
        return groupMemberRepository.findByMember(member).stream()
                .map(gm -> new GroupResponse(
                        gm.getGroup().getId(),
                        gm.getGroup().getName(),
                        gm.getGroup().getMotto(),
                        gm.getGroup().getLeader().getNickname(),
                        groupMemberRepository.countByGroup(gm.getGroup())
                )).toList();
    }

    public void removeOrLeaveMember(Member requester, Long groupId, Long targetId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        GroupMember targetGM = groupMemberRepository.findByGroupAndMember(group, memberRepository.findById(targetId).orElseThrow()).orElseThrow();

        if (group.getLeader().getId().equals(requester.getId()) || requester.getId().equals(targetId)) {
            if (group.getLeader().getId().equals(targetId)) throw new RuntimeException("리더는 탈퇴할 수 없습니다.");
            groupMemberRepository.delete(targetGM);
        } else {
            throw new RuntimeException("권한이 없습니다.");
        }
    }

    // 그룹 가입 요청
    public void requestToJoin(Member user, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow();

        if (groupMemberRepository.existsByGroupAndMember(group, user)) {
            throw new RuntimeException("이미 그룹 멤버입니다.");
        }
        // 중복 가입 요청 췍
        boolean alreadyRequested = groupJoinRequestRepository.existsByGroupAndRequesterAndStatus(
                group, user, JoinStatus.PENDING
        );
        if (alreadyRequested) {
            throw new RuntimeException("이미 가입 승인 대기 중입니다.");
        }
        groupJoinRequestRepository.save(GroupJoinRequest.builder()
                .group(group).requester(user).status(JoinStatus.PENDING).build());
    }

    // +참여 요청 승인?
    public void acceptJoinRequest(Member leader, Long requestId) {
        GroupJoinRequest joinReq = groupJoinRequestRepository.findById(requestId).orElseThrow();
        Group group = joinReq.getGroup();

        if (!group.getLeader().getId().equals(leader.getId())) throw new RuntimeException("권한 부족");
        if (groupMemberRepository.countByGroup(group) >= 5) throw new RuntimeException("인원 초과");

        joinReq.accept();
        groupMemberRepository.save(GroupMember.builder().group(group).member(joinReq.getRequester()).build());
    }

    // 그룹 상세 조회
    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupDetail(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("그룹을 찾을 수 없습니다."));

        List<GroupMember> groupMembers = groupMemberRepository.findByGroup(group);
        List<Member> members = groupMembers.stream().map(GroupMember::getMember).toList();
        long totalQuoteCount = quoteRepository.countByAuthorIn(members);

        List<GroupDetailResponse.MemberInfo> memberInfos = members.stream()
                .map(m -> new GroupDetailResponse.MemberInfo(m.getId(), m.getNickname()))
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

    // 그룹 메시지 수정
    public void updateMotto(Member requester, Long groupId, String newMotto) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("그룹을 찾을 수 없습니다."));

        boolean isMember = groupMemberRepository.existsByGroupAndMember(group, requester);
        if (!isMember) {
            throw new RuntimeException("그룹 멤버만 좌우명을 수정할 수 있습니다.");
        }
        group.setMotto(newMotto);
    }
}