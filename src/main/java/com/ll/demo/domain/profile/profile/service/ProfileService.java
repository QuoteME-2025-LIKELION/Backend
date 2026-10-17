package com.ll.demo.domain.profile.profile.service;

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

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
    private final MemberRepository memberRepository;
    private final QuoteRepository quoteRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;

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

        notificationRepository.deleteAllByMember(persistentMember);
        notificationSettingRepository.deleteByMember(persistentMember);
        memberRepository.delete(persistentMember);
    }
}