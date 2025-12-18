package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.group.group.dto.GroupSearchResponse;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.dto.FriendResponse;
import com.ll.demo.domain.member.member.dto.MemberSearchResponse;
import com.ll.demo.domain.member.member.dto.ProfileResponse;
import com.ll.demo.domain.member.member.dto.ProfileUpdateRequest;
import com.ll.demo.domain.member.member.dto.SearchCombinedResponse;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
import com.ll.demo.domain.quote.entity.QuoteTag;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRepository;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.AuthTokenService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final AuthTokenService authTokenService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final FriendshipRepository friendshipRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteLikeRepository quoteLikeRepository;
    private final QuoteTagRepository quoteTagRepository;

    // 이메일로 회원 조회
    @Transactional(readOnly = true)
    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    @Transactional
    public RsData<Member> join(String email, String password, String birthYear) {
        memberRepository.findByEmail(email).ifPresent(ignored -> {
            throw new GlobalException("400-1", "이미 존재하는 이메일");
        });
        // 닉네임 자동 생성 - 리팩토링?
        String nickname = email.split("@")[0];

        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .birthYear(birthYear)
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
    public void updateProfile(Long memberId, ProfileUpdateRequest request, String imageUrl) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));

        // 1. 닉네임, 소개글 변경 (기존 로직 유지)
        member.setNickname(request.getNickname());
        member.setIntroduction(request.getIntroduction());

        // 2. 프로필 이미지 변경 (수정된 핵심 로직)
        // 컨트롤러에서 넘겨준 S3 URL이 있다면, 그것으로 DB를 업데이트합니다.
        // (imageUrl이 null이면, 새 사진을 안 올렸다는 뜻이니 기존 사진을 유지합니다.)
        if (imageUrl != null && !imageUrl.isEmpty()) {
            member.setProfileImage(imageUrl);
        }
    }

//    // 닉네임, 이메일, 그룹명으로 회원 검색
//    public List<MemberSearchResponse> searchMembers(String keyword, Long currentMemberId) {
//        List<Member> membersByNicknameOrEmail = memberRepository
//                .searchMembersByNicknameOrEmailUsername(keyword);
//        List<Member> membersByGroupName = groupMemberRepository
//                .findMembersByGroupNameContaining(keyword);
//        Set<Member> combinedMembers = new HashSet<>(membersByNicknameOrEmail);
//        combinedMembers.addAll(membersByGroupName);
//
//        List<Member> filteredMembers = combinedMembers.stream()
//                .filter(m -> !m.getId().equals(currentMemberId))
//                .toList();
//
//        return filteredMembers.stream()
//                .map(MemberSearchResponse::of)
//                .toList();
//    }

    // 회원 및 그룹 검색
    public SearchCombinedResponse searchCombined(String keyword, Long currentMemberId) {
        // 닉네임or이메일로 검색
        List<Member> membersByNicknameOrEmail = memberRepository
                .searchMembersByNicknameOrEmailUsername(keyword);

        // 그룹명으로 그룹 멤버 검색
        List<Member> membersByGroupName = groupMemberRepository
                .findMembersByGroupNameContaining(keyword);

        Set<Member> combinedMembers = new HashSet<>(membersByNicknameOrEmail);
        combinedMembers.addAll(membersByGroupName);

        List<MemberSearchResponse> memberResponses = combinedMembers.stream()
                .filter(m -> !m.getId().equals(currentMemberId))
                .map(MemberSearchResponse::of)
                .toList();
        // 그룹 자체
        List<Group> groups = groupRepository.findByNameContainingIgnoreCase(keyword);

        List<GroupSearchResponse> groupResponses = groups.stream()
                .map(GroupSearchResponse::of)
                .toList();

        return SearchCombinedResponse.builder()
                .members(memberResponses)
                .groups(groupResponses)
                .build();
    }


    // 친구 목록 조회
    public List<FriendResponse> getFriendList(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));

        List<Friendship> friendships = friendshipRepository.findAllByMember(member);

        return friendships.stream()
                .map(friendship -> {
                    Member friend = friendship.getFriend();
                    return FriendResponse.of(friend, false);
                })
                .toList();
    }

    // 리프레시 토큰 생성 저장
    @Transactional
    public String genRefreshToken(Member member) {
        String refreshToken = authTokenService.genToken(member, 60 * 60 * 24 * 30); // 30일
        member.setRefreshToken(refreshToken);
        // 강제로 DB반영
        memberRepository.saveAndFlush(member);
        return refreshToken;
    }

    // 이하 게스트 초기 데이터 생성
    @Transactional
    public Member findOrCreateGuest() {
        return memberRepository.findByEmail("guest@guest.com")
                .orElseGet(() -> {
                    // 게스트 계정 생성 > 프로필 풀세팅
                    Member guest = Member.builder()
                            .email("guest@guest.com")
                            .password(passwordEncoder.encode("guest1234"))
                            .nickname("듀")
                            .birthYear("2000")
                            .introduction("휴학하고싶다")
                            .profileImage("https://img1.daumcdn.net/thumb/R1280x0.fwebp/?fname=http://t1.daumcdn.net/brunch/service/user/cnoC/image/0FLb5BJ8prwjPqpPVzqxfpfRpuU")
                            .build();
                    memberRepository.save(guest);

                    setupGuestDemoData(guest);

                    return guest;
                });
    }

    private void setupGuestDemoData(Member guest) {
        // 사전 데이터 생성 여부 검사
        if (memberRepository.findByEmail("kju@test.com").isPresent()) return;

        // 이미지 주소는 저작권 문제로 추후 리팩터링 필요
        Member kju = createDemoMember(
                "kju@test.com", "김쮸", "2008", "퇴근시켜주세요",
                "https://t1.daumcdn.net/brunch/service/user/cnoC/image/hIqgJajCFnhylAsgxinbLvVfANA"
        );
        Member haeoni = createDemoMember(
                "haeoni@test.com", "해오니", "2002", "집에가자!!!",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ1wB4JGh0h8oTTvyogDGiqGW877Vv2DbQBfA&s"
        );
        Member jjang = createDemoMember(
                "jjang@test.com", "짱규진", "2006", "말차하임존맛",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQQfIDMian0WOUYiJAIPYkGVpa7itYY-3ZMzQ&s"
        );

        List<Member> sunshines = Arrays.asList(kju, haeoni, jjang);

            // 게스트, 기존 사용자들 all 친구
            for (Member friend : sunshines) {
                makeFriendship(guest, friend);
            }

            // 기존 그룹 생성 - saveAndFlush 이용하여 id 확정
            Group sunshineGroup = groupRepository.saveAndFlush(Group.builder()
                    .name("햇살즈")
                    .motto("휴학plz")
                    .leader(kju)
                    .build());

            List<Member> allMembers = new ArrayList<>(sunshines);
            allMembers.add(guest);

            for (Member m : allMembers) {
                groupMemberRepository.save(new GroupMember(sunshineGroup, m));
            }

            // 기존 명언 생성 - 좋아요, 태그 포함
            // 오늘
            LocalDateTime now = LocalDateTime.now(); // 기준 시간 - 서버 실행 시점의 오늘 날짜
            Quote q1 = createDemoQuote(kju, "가장 빛나는 별은 아직 발견되지 않은 별이다", "아 완전 뒤처진 것 같음 근데 아직 젊으니까 미래는 창창한 거 아닌가?", now.minusHours(3));
            Quote q2 = createDemoQuote(jjang, "진정한 용기는 두려움을 느끼지 않는 것이 아니라 두려움을 느끼면서도 해내는 것이다", "어제 겁나서 도망갈뻔했는데 내가 해냄", now.minusHours(1));
            // 어제
            LocalDateTime yesterday = now.minusDays(1);
            Quote q7 = createDemoQuote(guest, "가장 어두운 밤이 지나고 나면, 가장 빛나는 새벽이 온다", "매일매일 남들 놀러다닐 때 나만 공부하느라 처박혀 있느라 힘들었고 울기도 많이 울었다. 그런데 오늘 1차 붙었다는 소식 받으니까 진짜 그 고생이 다 이걸 위해서인 것 같아서 눈물이 또 남...ㅎㅎ", yesterday.withHour(4));
            Quote q3 = createDemoQuote(haeoni, "어제보다 나은 오늘은 내가 만들어가는 것", "어제 열역학 공부하느라 밤새고 밥도 맛없는거 먹어서 우울했다... 오늘 12시간 자고 애들이랑 놀고왔더니 훨씬 낫다.", yesterday.withHour(10));
            Quote q4 = createDemoQuote(jjang, "아름답지 않은 것에서 발견하는 아름다움", "엄마가 사준 옷 너무 못생김 근데 엄마 마음이 예쁘다 생각하고 걍 입으려고 근데 밖에서는 못입을거같아ㅠㅠ", yesterday.withHour(13));
            Quote q5 = createDemoQuote(kju, "중요한 것은 성공하는 능력보다 실패를 거듭하는 능력이다", "다이어트 중인데 어제 엽떡먹고 오늘 두쫀쿠 4개먹음ㅋㅋㅋㅋㅋ 근데 오늘부터 다시 시작하면 됨", yesterday.withHour(22));
            // 이틀 전
            LocalDateTime twoDaysAgo = now.minusDays(2);
            Quote q6 = createDemoQuote(haeoni, "포기하는 순간 시합은 종료야", "시험 3시간 남았는데 이제 개강!! 벌써 망한것같지만 일단 끝까지 드가자 여법러들아 다죽자", twoDaysAgo.withHour(15));
            Quote q8 = createDemoQuote(guest, "자신에게 너그럽고 친절하라, 자신을 사랑하라", "너무 뚱뚱해서 딱 붙는 옷 절대 못 입었는데 오늘 눈 딱 감고 붙는 티 입었더니 생각보다 괜찮았다 내가 나한테 너무 소홀했던 것 같다 나한테 잘해줘야겠다 생각을 했다", twoDaysAgo.withHour(15));


            quoteLikeRepository.save(new QuoteLike(q1, haeoni));
            quoteLikeRepository.save(new QuoteLike(q1, jjang));
            quoteLikeRepository.save(new QuoteLike(q3, kju));

            quoteTagRepository.save(new QuoteTag(q1, guest));
            quoteTagRepository.save(new QuoteTag(q2, kju));
            quoteTagRepository.save(new QuoteTag(q2, jjang));
            quoteTagRepository.save(new QuoteTag(q2, guest));
            quoteTagRepository.save(new QuoteTag(q3, haeoni));
            quoteTagRepository.save(new QuoteTag(q3, guest));
    }

    private Member createDemoMember(String email, String nickname, String birthYear, String intro, String profileImage) {
        return memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .email(email)
                                .password(passwordEncoder.encode("demo1234"))
                                .nickname(nickname)
                                .birthYear(birthYear)
                                .introduction(intro)
                                .profileImage(profileImage)
                                .build()
                ));
    }

    private void makeFriendship(Member m1, Member m2) {
        friendshipRepository.save(Friendship.builder().member(m1).friend(m2).status(FriendshipStatus.ACCEPTED).build());
        friendshipRepository.save(Friendship.builder().member(m2).friend(m1).status(FriendshipStatus.ACCEPTED).build());
    }

    private Quote createDemoQuote(Member author, String content, String original, LocalDateTime createdAt) {
        Quote quote = Quote.builder()
                .author(author)
                .content(content)
                .originalContent(original)
                .build();
        quoteRepository.saveAndFlush(quote);

        quote.setCreateDateForDemo(createdAt);

        return quoteRepository.saveAndFlush(quote);
    }

    public void save(Member member) {
        memberRepository.save(member);
    }
}
