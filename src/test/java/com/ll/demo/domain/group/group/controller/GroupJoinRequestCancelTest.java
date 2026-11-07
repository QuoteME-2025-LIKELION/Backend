package com.ll.demo.domain.group.group.controller;

import com.ll.demo.domain.group.group.dto.GroupRequest;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupJoinRequest;
import com.ll.demo.domain.group.group.entity.InviteType;
import com.ll.demo.domain.group.group.entity.JoinStatus;
import com.ll.demo.domain.group.group.repository.GroupJoinRequestRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.group.group.service.GroupService;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupJoinRequestCancelTest {

    @Autowired private MockMvc mvc;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private GroupService groupService;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupJoinRequestRepository groupJoinRequestRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private EntityManager em;

    private Member leader;
    private Member applicant;
    private Member other;
    private Long groupId;

    @BeforeEach
    void setUp() {
        leader = memberRepository.save(Member.builder().email("leader@test.com").nickname("리더").password("password123").build());
        applicant = memberRepository.save(Member.builder().email("applicant@test.com").nickname("신청자").password("password123").build());
        other = memberRepository.save(Member.builder().email("other@test.com").nickname("남").password("password123").build());
        groupId = groupService.createGroup(leader, new GroupRequest("취소모임", null)).id();
    }

    private ResultActions cancel(Member actor, Long requestId) throws Exception {
        return mvc.perform(delete("/api/groups/join-requests/" + requestId)
                .header("Authorization", "Bearer " + authTokenService.genToken(actor, 600)));
    }

    private void flushClear() {
        em.flush();
        em.clear();
    }

    private Long requestJoin(Member user) {
        groupService.requestToJoin(user, groupId);
        flushClear();
        Group group = groupRepository.findById(groupId).orElseThrow();
        return groupJoinRequestRepository.findByGroupAndRequesterAndStatus(group, user, JoinStatus.PENDING).orElseThrow().getId();
    }

    private long joinRequestNotifications() {
        return notificationRepository.findByReceiverIdAndTypeOrderByCreateDateDesc(leader.getId(), "GROUP_JOIN_REQUEST").size();
    }

    @Test
    @DisplayName("본인의 대기 가입 신청 취소 → 행과 그룹장 알림 삭제")
    void cancel_deletesRequestAndNotification() throws Exception {
        Long requestId = requestJoin(applicant);
        assertThat(joinRequestNotifications()).isEqualTo(1);

        cancel(applicant, requestId).andExpect(status().isOk());
        flushClear();

        assertThat(groupJoinRequestRepository.findById(requestId)).isEmpty();
        assertThat(joinRequestNotifications()).isZero();
    }

    @Test
    @DisplayName("다른 사람의 신청 취소 → 403")
    void cancelOthers_403() throws Exception {
        Long requestId = requestJoin(applicant);

        cancel(other, requestId).andExpect(status().isForbidden());
        flushClear();

        assertThat(groupJoinRequestRepository.findById(requestId)).isPresent();
    }

    @Test
    @DisplayName("수락된 신청 취소 → 400")
    void cancelAccepted_400() throws Exception {
        Long requestId = requestJoin(applicant);
        groupService.acceptJoinRequest(memberRepository.findById(leader.getId()).orElseThrow(), requestId);
        flushClear();

        cancel(applicant, requestId).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("초대(INVITE) 유형은 이 API로 취소할 수 없다 → 400")
    void cancelInvite_400() throws Exception {
        Group group = groupRepository.findById(groupId).orElseThrow();
        GroupJoinRequest invite = groupJoinRequestRepository.save(GroupJoinRequest.builder()
                .group(group).inviter(leader).requester(applicant)
                .status(JoinStatus.PENDING).type(InviteType.INVITE).build());
        flushClear();

        cancel(applicant, invite.getId()).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("없는 신청 → 404")
    void cancelMissing_404() throws Exception {
        cancel(applicant, 999_999L).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("취소 후 같은 그룹에 다시 신청할 수 있다")
    void reRequestAfterCancel() throws Exception {
        Long requestId = requestJoin(applicant);
        cancel(applicant, requestId).andExpect(status().isOk());
        flushClear();

        Long again = requestJoin(memberRepository.findById(applicant.getId()).orElseThrow());

        assertThat(again).isNotEqualTo(requestId);
        assertThat(joinRequestNotifications()).isEqualTo(1);
    }

    @Test
    @DisplayName("보낸 가입 신청 목록에는 대기 건만 표시된다")
    void sentList_pendingOnly() throws Exception {
        Long pendingId = requestJoin(applicant);
        Long otherGroupId = groupService.createGroup(other, new GroupRequest("다른모임", null)).id();
        groupService.requestToJoin(applicant, otherGroupId);
        flushClear();
        Group otherGroup = groupRepository.findById(otherGroupId).orElseThrow();
        Long rejectedId = groupJoinRequestRepository.findByGroupAndRequesterAndStatus(otherGroup, applicant, JoinStatus.PENDING).orElseThrow().getId();
        groupService.rejectJoinRequest(memberRepository.findById(other.getId()).orElseThrow(), rejectedId);
        flushClear();

        mvc.perform(get("/api/groups/join-requests/sent")
                        .header("Authorization", "Bearer " + authTokenService.genToken(applicant, 600)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].requestId").value(pendingId))
                .andExpect(jsonPath("$[0].groupId").value(groupId))
                .andExpect(jsonPath("$[0].groupName").value("취소모임"))
                .andExpect(jsonPath("$[0].requestedAt").exists());
    }
}
