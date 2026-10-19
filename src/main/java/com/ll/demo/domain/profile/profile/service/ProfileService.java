package com.ll.demo.domain.profile.profile.service;

import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.repository.GroupJoinRequestRepository;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.poke.repository.PokeRepository;
import com.ll.demo.domain.quote.repository.BookmarkRepository;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRequestRepository;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.profile.profile.dto.AccountInfoResponse;
import com.ll.demo.domain.profile.profile.dto.ProfileResponse;
import com.ll.demo.domain.profile.profile.dto.ProfileUpdateRequest;
import com.ll.demo.domain.profile.profile.dto.AccountUpdateRequest;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
    private final MemberRepository memberRepository;
    private final QuoteRepository quoteRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
        private static final String GUEST_EMAIL = "guest@guest.com";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final QuoteLikeRepository quoteLikeRepository;
    private final QuoteTagRepository quoteTagRepository;
    private final QuoteTagRequestRepository quoteTagRequestRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PokeRepository pokeRepository;

    // 프로필 정보 조회
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(Member member) {
        long quoteCount = quoteRepository.countByAuthor(member);
        long friendCount = friendshipRepository.countByMemberAndStatus(member, FriendshipStatus.ACCEPTED);
        return ProfileResponse.from(member, quoteCount, friendCount);
    }

    // 프로필 정보 수정 (imageUrl: S3 업로드 후 받은 URL, null이면 기존 값 유지)
    public ProfileResponse updateProfile(Member member, ProfileUpdateRequest request, String imageUrl) {
        Member persistentMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (request != null) {
            if (request.nickname() != null && !request.nickname().isBlank()
                    && !request.nickname().equals(persistentMember.getNickname())) {
                memberRepository.findByNickname(request.nickname())
                        .ifPresent(m -> { throw new IllegalArgumentException("이미 사용 중인 닉네임입니다."); });
                persistentMember.setNickname(request.nickname());
            }
            if (request.introduction() != null) {
                persistentMember.setIntroduction(request.introduction());
            }
        }

        // S3 URL 우선, 없으면 JSON의 profileImage 필드 사용
        if (imageUrl != null && !imageUrl.isBlank()) {
            persistentMember.setProfileImage(imageUrl);
        } else if (request != null && request.profileImage() != null) {
            persistentMember.setProfileImage(request.profileImage());
        }

        return getMyProfile(persistentMember);
    }

    // 계정 정보 수정
    public void updateAccountInfo(Member member, AccountUpdateRequest request) {
        Member persistentMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 이메일 중복 체크
        if (!persistentMember.getEmail().equals(request.email())) {
            memberRepository.findByEmail(request.email()).ifPresent(m -> {
                throw new RuntimeException("이미 사용 중인 이메일입니다.");
            });
        }

        persistentMember.setEmail(request.email());
        persistentMember.setBirthYear(request.birthYear());
        persistentMember.setGender(request.gender()); // 성별 업데이트
    }

    // 계정 정보 조회 (소셜 연동 상태 포함)
    @Transactional(readOnly = true)
    public AccountInfoResponse getAccountInfo(Member member) {
        Member persistentMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return AccountInfoResponse.from(persistentMember);
    }
    // 계정 삭제
    public void deleteAccount(Member member) {
        Member persistentMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (GUEST_EMAIL.equals(persistentMember.getEmail())) {
            throw new GlobalException("403", "게스트 계정은 탈퇴할 수 없습니다.");
        }

        handleLedGroups(persistentMember);

        bookmarkRepository.deleteAllRelatedToMember(persistentMember);
        quoteLikeRepository.deleteAllRelatedToMember(persistentMember);
        quoteTagRepository.deleteAllRelatedToMember(persistentMember);
        quoteTagRequestRepository.deleteAllRelatedToMember(persistentMember);
        quoteRepository.deleteAllByAuthor(persistentMember);
        friendshipRepository.deleteAllByMemberOrFriend(persistentMember);
        groupJoinRequestRepository.deleteAllByMember(persistentMember);
        groupMemberRepository.deleteAllByMember(persistentMember);
        pokeRepository.deleteAllByMember(persistentMember);
        notificationRepository.deleteAllByMember(persistentMember);
        notificationSettingRepository.deleteByMember(persistentMember);
        memberRepository.delete(persistentMember);
    }

    // rPwjd tkrwp
    private void handleLedGroups(Member member) {
        for (Group group : groupRepository.findAllByLeader(member)) {
            Optional<Member> successor = groupMemberRepository.findByGroup(group).stream()
                    .filter(groupMember -> !groupMember.getMember().getId().equals(member.getId()))
                    .min(Comparator.comparing(GroupMember::getId))
                    .map(GroupMember::getMember);
            if (successor.isPresent()) {
                group.setLeader(successor.get());
            } else {
                groupRepository.delete(group);
            }
        }
        groupRepository.flush();
    }
}