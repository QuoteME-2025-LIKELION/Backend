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
import com.ll.demo.domain.member.member.type.MemberProvider;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
import com.ll.demo.domain.quote.entity.QuoteTag;
import com.ll.demo.domain.notice.entity.Notice;
import com.ll.demo.domain.notice.entity.NoticeType;
import com.ll.demo.domain.notice.repository.NoticeRepository;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRepository;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.domain.quote.repository.QuoteTagRequestRepository;
import com.ll.demo.domain.quote.entity.TagRequestStatus;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import java.time.LocalDate;
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
    private final QuoteTagRequestRepository quoteTagRequestRepository;
    private final NoticeRepository noticeRepository;

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
// MemberService.java

@Transactional
public void updateProfile(Long memberId, ProfileUpdateRequest request, String imageUrl) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));

    // 값이 있는 필드만 수정하도록 로직 수정
    if (request != null) {
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            member.setNickname(request.getNickname());
        }
        if (request.getIntroduction() != null) {
            member.setIntroduction(request.getIntroduction());
        }
    }

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

    // AI 추천 횟수 체크 + 증가 (일 3회 제한, 날짜 바뀌면 초기화)
    @Transactional
    public void checkAndIncrementAiUsage(Member member) {
        LocalDate today = LocalDate.now();

        if (!today.equals(member.getAiUsageDate())) {
            member.setAiUsageDate(today);
            member.setAiUsageCount(0);
        }

        if (member.getAiUsageCount() >= 3) {
            throw new GlobalException("429-1", "AI 추천은 하루 3회까지만 사용할 수 있습니다.");
        }

        member.setAiUsageCount(member.getAiUsageCount() + 1);
        memberRepository.save(member);
    }

    // 소셜 로그인: 기존 회원 조회 or 신규 생성
    @Transactional
    public Member findOrCreateSocialMember(
            MemberProvider provider, String providerId,
            String email, String nickname, String profileImage) {

        // 1. provider + providerId로 조회
        Optional<Member> existing = memberRepository.findByProviderAndProviderId(provider, providerId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. 동일 이메일의 LOCAL 계정이 있으면 provider 연결 후 반환
        if (email != null) {
            Optional<Member> localMember = memberRepository.findByEmail(email);
            if (localMember.isPresent()) {
                Member m = localMember.get();
                m.setProvider(provider);
                m.setProviderId(providerId);
                return memberRepository.save(m);
            }
        }

        // 3. 신규 소셜 회원 생성
        String resolvedNickname = (nickname != null && !nickname.isBlank()) ? nickname : (email != null ? email.split("@")[0] : "user");
        Member newMember = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .nickname(resolvedNickname)
                .profileImage(profileImage)
                .provider(provider)
                .providerId(providerId)
                .build();
        return memberRepository.save(newMember);
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

                    noticeRepository.save(Notice.builder()
                            .type(NoticeType.NOTICE)
                            .title("앱 출시 안내")
                            .content("QuoteMe 앱이 정식 출시되었습니다!면 좋겠다")
                            .build());

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
            Quote q1 = createDemoQuote(
                kju, 
                "가장 빛나는 별은 아직 발견되지 않은 별이다", 
                "아 완전 뒤처진 것 같음 근데 아직 젊으니까 미래는 창창한 거 아닌가?", 
                now.minusHours(3));
            Quote q2 = createDemoQuote(
                jjang, 
                "진정한 용기는 두려움을 느끼지 않는 것이 아니라 두려움을 느끼면서도 해내는 것이다", 
                "어제 겁나서 도망갈뻔했는데 내가 해냄", 
                now.minusHours(1));
            // 어제
            LocalDateTime yesterday = now.minusDays(1);
            Quote q7 = createDemoQuote(
                guest, 
                "어디선가 보이지 않던 것들이, 다른 곳에선 삶의 일부가 된다.", 
                "독일 여행을 할 때는 고양이를 본 적이 없는데 튀니지에는 고양이가 참 많다 그래서 좋다", 
                yesterday.withHour(4));
            Quote q3 = createDemoQuote(
                haeoni, 
                "멈추면 비로소 삶을 진실로 알게 되리라.", 
                "나는 하루종일 누워있는 게 적성에 딱맞음 진심", 
                yesterday.withHour(10));
            Quote q4 = createDemoQuote(
                jjang, 
                "기침과 사랑은 숨겨지지 않는다", 
                "두쫀쿠 너무 열정적으로 먹었나봐 코코아가루 뿜고 옷에 다 묻음",
                yesterday.withHour(13));
            Quote q5 = createDemoQuote(
                kju, 
                "이 길이 멀고 험한 이유는, 그 끝에 누구도 모르는 세상이 기다리고 있기 때문이다.", 
                "혼자 이렇게 떨어져 있으니까 너무 힘들다 하지만 생각을 바꿔서 열심히 하기로 함!!!", 
                yesterday.withHour(22));
            // 이틀 전
            LocalDateTime twoDaysAgo = now.minusDays(2);
            Quote q6 = createDemoQuote(
                haeoni, 
                "삶의 가장 커다란 기쁨은 사랑하는 사람과의 사소한 시간이다.", 
                "고구마 잘못 사서 엄마랑 엄청 웃었다!!! 별거 아닌데 기뻤다", 
                twoDaysAgo.withHour(15));
            Quote q8 = createDemoQuote(
                guest, 
                "다시 일어설 힘은 항상 내 안에 있다.", 
                "​다이어트 콘텐츠 한다면서 살 4키로 뺐는데 술 먹어서 도로 2키로 찜 ㄱㅊ아. 이제 운동해야지...",
                twoDaysAgo.withHour(15));


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

    private Quote createDemoQuote(Member author, String summary, String original, LocalDateTime createdAt) {
        Quote quote = Quote.builder()
                .author(author)
                .content(summary)           // ai 명언
                .originalContent(original)  // 원본 일기 내용
                .summary(summary)           // dto 호환 - 리팩토링 시 정리?
                .build();
        
        quoteRepository.saveAndFlush(quote);
        quote.setCreateDateForDemo(createdAt);

        return quoteRepository.saveAndFlush(quote);
    }
    public void save(Member member) {
        memberRepository.save(member);
    }
}
