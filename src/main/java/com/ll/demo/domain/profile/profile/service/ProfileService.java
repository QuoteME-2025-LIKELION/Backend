package com.ll.demo.domain.profile.profile.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.profile.profile.dto.ProfileResponse;
import com.ll.demo.domain.profile.profile.dto.ProfileUpdateRequest;
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

    // 프로필 정보 조회
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(Member member) {
        long quoteCount = quoteRepository.countByAuthor(member);
        long friendCount = friendshipRepository.countByMember(member);
        return ProfileResponse.from(member, quoteCount, friendCount);
    }

    // 프로필 정보 수정
    public ProfileResponse updateProfile(Member member, ProfileUpdateRequest request) {
        Member persistentMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (request.nickname() != null && !request.nickname().equals(persistentMember.getNickname())) {
            memberRepository.findByNickname(request.nickname())
                    .ifPresent(m -> { throw new IllegalArgumentException("이미 사용 중인 닉네임입니다."); });
        }

        // 객체 정보 수정
        persistentMember.setNickname(request.nickname());
        persistentMember.setIntroduction(request.introduction());
        persistentMember.setProfileImage(request.profileImage());
        // memberRepository.save(persistentMember);

        return getMyProfile(persistentMember);
    }
}