package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.rsData.RsData;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ll.demo.domain.member.member.dto.ProfileResponse;
import com.ll.demo.domain.member.member.dto.ProfileUpdateRequest;
import com.ll.demo.domain.member.member.dto.MemberSearchResponse;
import com.ll.demo.domain.member.member.dto.FriendResponse;
import com.ll.demo.domain.member.member.repository.FriendshipRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import com.ll.demo.domain.member.member.entity.Friendship;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final FriendshipRepository friendshipRepository;

    // 이메일로 회원 조회
    @Transactional(readOnly = true)
    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    // 회원가입
    @Transactional
    public RsData<Member> join(String email, String password, Integer birthYear) {
        // 이메일 중복 체크
        findByEmail(email).ifPresent(ignored -> {
            throw new GlobalException("400-1", "이미 존재하는 이메일");
        });

        // 새로운 회원 객체
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))  // 비밀번호 암호화
                .birthYear(String.valueOf(birthYear))
                .build();
        memberRepository.save(member);
        return RsData.of("회원가입 완료", member);
    }

    // ID로 회원 조회?
    @Transactional(readOnly = true)
    public Member getMemberById(long id) {
        return memberRepository.findById(id).orElseThrow(() -> new GlobalException("400-2", "회원이 존재하지 않습니다."));
    }

    // RefreshToken 메서드
    @Transactional
    public java.util.Optional<Member> findByRefreshToken(String refreshToken) {
        return memberRepository.findByRefreshToken(refreshToken);
    }

    // 비번 일치 확인 메서드
    @Transactional(readOnly = true)
    public boolean checkPassword(Member member, String rawPassword) {
        return passwordEncoder.matches(rawPassword, member.getPassword());
    }

    public Optional<Member> findById(long id) {
        return memberRepository.findById(id);
    }

    // 내 프로필 조회
    public ProfileResponse getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));

        return ProfileResponse.of(member);
    }

    // 프로필 정보 수정
    @Transactional
    public void updateProfile(Long memberId, ProfileUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));

        String newProfileImageUrl = member.getProfileImage(); // 기존 URL 유지

        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            // 임시 파일 업로드 로직 - !재검토 필요!
            // String uploadedUrl = fileStorageService.upload(request.getProfileImage());
            // newProfileImageUrl = uploadedUrl;
            newProfileImageUrl = "/images/profile/" + memberId + "_new_image.jpg"; // 임시 URL
        }
        // 기존 이미지 삭제?

        member.setNickname(request.getNickname());
        member.setIntroduction(request.getIntroduction());
        member.setProfileImage(newProfileImageUrl);
    }
    // 닉네임 or 이메일로 회원 검색
    // 친구 목록 조회 + 친구 검색
    public List<MemberSearchResponse> searchMembers(String keyword, Long currentMemberId) {

        List<Member> members = memberRepository
                .searchMembersByNicknameOrEmailUsername(keyword);

        List<Member> filteredMembers = members.stream()
                .filter(m -> !m.getId().equals(currentMemberId))
                .toList();

        return filteredMembers.stream()
                .map(MemberSearchResponse::of)
                .toList();
    }

    // 친구 목록 조회
    public List<FriendResponse> getFriendList(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));

        List<Friendship> friendships = friendshipRepository.findAllByMember(member);

        return friendships.stream()
                .map(Friendship::getFriend)
                .map(FriendResponse::of)
                .toList();
    }

}
