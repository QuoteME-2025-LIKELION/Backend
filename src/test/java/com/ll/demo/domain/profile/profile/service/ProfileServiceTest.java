package com.ll.demo.domain.profile.profile.service;

import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.repository.GroupJoinRequestRepository;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import com.ll.demo.domain.poke.repository.PokeRepository;
import com.ll.demo.domain.quote.repository.BookmarkRepository;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRequestRepository;
import com.ll.demo.global.exceptions.GlobalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationSettingRepository notificationSettingRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private GroupJoinRequestRepository groupJoinRequestRepository;
    @Mock private QuoteLikeRepository quoteLikeRepository;
    @Mock private QuoteTagRepository quoteTagRepository;
    @Mock private QuoteTagRequestRepository quoteTagRequestRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private PokeRepository pokeRepository;

    @InjectMocks
    private ProfileService profileService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder().nickname("탈퇴자").build();
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    private Member memberWithId(long id) {
        Member other = Member.builder().nickname("회원" + id).build();
        ReflectionTestUtils.setField(other, "id", id);
        return other;
    }

    private GroupMember groupMember(Group group, Member groupMember, long id) {
        GroupMember gm = new GroupMember(group, groupMember);
        ReflectionTestUtils.setField(gm, "id", id);
        return gm;
    }

    @Test
    @DisplayName("성공: 탈퇴 시 연관 데이터를 FK 의존 순서대로 삭제한 뒤 회원을 삭제한다")
    void deleteAccount_deletesRelatedDataInOrder() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        profileService.deleteAccount(member);

        InOrder inOrder = inOrder(groupRepository, bookmarkRepository, quoteLikeRepository, quoteTagRepository,
                quoteTagRequestRepository, quoteRepository, friendshipRepository, groupJoinRequestRepository,
                groupMemberRepository, pokeRepository, notificationRepository, notificationSettingRepository,
                memberRepository);
        inOrder.verify(groupRepository).findAllByLeader(member);
        inOrder.verify(bookmarkRepository).deleteAllRelatedToMember(member);
        inOrder.verify(quoteLikeRepository).deleteAllRelatedToMember(member);
        inOrder.verify(quoteTagRepository).deleteAllRelatedToMember(member);
        inOrder.verify(quoteTagRequestRepository).deleteAllRelatedToMember(member);
        inOrder.verify(quoteRepository).deleteAllByAuthor(member);
        inOrder.verify(friendshipRepository).deleteAllByMemberOrFriend(member);
        inOrder.verify(groupJoinRequestRepository).deleteAllByMember(member);
        inOrder.verify(groupMemberRepository).deleteAllByMember(member);
        inOrder.verify(pokeRepository).deleteAllByMember(member);
        inOrder.verify(notificationRepository).deleteAllByMember(member);
        inOrder.verify(notificationSettingRepository).deleteByMember(member);
        inOrder.verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("성공: 리더인 그룹에 다른 멤버가 있으면 가장 먼저 가입한 멤버에게 리더를 위임한다")
    void deleteAccount_ledGroupWithMembers_transfersLeaderToEarliestMember() {
        Member earlier = memberWithId(2L);
        Member later = memberWithId(3L);
        Group group = Group.builder().name("그룹").leader(member).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(groupRepository.findAllByLeader(member)).thenReturn(List.of(group));
        when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(
                groupMember(group, member, 10L),
                groupMember(group, later, 12L),
                groupMember(group, earlier, 11L)));

        profileService.deleteAccount(member);

        assertThat(group.getLeader()).isSameAs(earlier);
        verify(groupRepository, never()).delete(any());
        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("성공: 리더인 그룹에 혼자 남아 있으면 그룹을 삭제한다")
    void deleteAccount_ledGroupAlone_deletesGroup() {
        Group group = Group.builder().name("그룹").leader(member).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(groupRepository.findAllByLeader(member)).thenReturn(List.of(group));
        when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(groupMember(group, member, 10L)));

        profileService.deleteAccount(member);

        verify(groupRepository).delete(group);
        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("실패: 게스트 계정은 탈퇴할 수 없고 아무것도 삭제하지 않는다")
    void deleteAccount_guest_forbidden() {
        Member guest = Member.builder().email("guest@guest.com").build();
        ReflectionTestUtils.setField(guest, "id", 99L);
        when(memberRepository.findById(99L)).thenReturn(Optional.of(guest));

        assertThatThrownBy(() -> profileService.deleteAccount(guest))
                .isInstanceOf(GlobalException.class)
                .hasMessageContaining("403");

        verify(memberRepository, never()).delete(any());
        verifyNoInteractions(groupRepository, bookmarkRepository, quoteRepository, notificationRepository);
    }

    @Test
    @DisplayName("실패: 회원이 없으면 예외가 발생하고 아무것도 삭제하지 않는다")
    void deleteAccount_memberNotFound_deletesNothing() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.deleteAccount(member))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(notificationRepository, notificationSettingRepository, groupRepository,
                bookmarkRepository, quoteLikeRepository, quoteTagRepository, quoteTagRequestRepository,
                quoteRepository, friendshipRepository, groupJoinRequestRepository, groupMemberRepository,
                pokeRepository);
        verify(memberRepository, never()).delete(any());
    }
}