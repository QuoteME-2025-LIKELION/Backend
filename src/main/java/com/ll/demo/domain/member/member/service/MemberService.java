package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.group.group.dto.GroupSearchResponse;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.dto.SocialLoginResult;
import com.ll.demo.domain.member.member.dto.FriendResponse;
import com.ll.demo.domain.member.member.dto.MemberSearchResponse;
import com.ll.demo.domain.member.member.dto.SearchCombinedResponse;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.entity.MemberSocialAccount;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.repository.MemberSocialAccountRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    // AI 추천 하루 한도 (custom.ai.daily-limit에서만 관리)
    @Value("${custom.ai.daily-limit:1}")
    private int aiDailyLimit;


    private static final int NICKNAME_MAX_LENGTH = 10;
    public static final String SOCIAL_ACCOUNT_NOT_FOUND = "account_not_found";
    public static final String SOCIAL_EMAIL_NOT_VERIFIED = "email_not_verified";
    public static final String SOCIAL_ALREADY_LINKED = "already_linked";
    public static final String SOCIAL_PROVIDER_ALREADY_LINKED = "provider_already_linked";

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
    private final MemberSocialAccountRepository memberSocialAccountRepository;

    // 이메일로 회원 조회
    @Transactional(readOnly = true)
    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    @Transactional
    public RsData<Member> join(String email, String password, String birthYear) {
        if (email.toLowerCase().endsWith("@kakao.user")) {
            throw new GlobalException("400-2", "사용할 수 없는 이메일입니다.");
        }
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
                .profileCompleted(false)
                .build();

        memberRepository.save(member);
        return RsData.of("회원가입 완료", member);
    }

    // ID로 회원 조회?
    @Transactional(readOnly = true)
    public Member getMemberById(long id) {
        return memberRepository.findById(id).orElseThrow(() -> new GlobalException("400-2", "회원이 존재하지 않습니다."));
    }

    // RefreshToken 메서드 (typ=access 토큰은 DB와 일치해도 refresh로 받지 않음)
    @Transactional
    public java.util.Optional<Member> findByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !authTokenService.isRefreshTokenType(refreshToken)) {
            return Optional.empty();
        }
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
        if (keyword == null || keyword.trim().isEmpty()) {
            return SearchCombinedResponse.builder().members(List.of()).groups(List.of()).build();
        }
        String likeKeyword = escapeLike(keyword);

        // 닉네임or이메일로 검색
        List<Member> membersByNicknameOrEmail = memberRepository
                .searchMembersByNicknameOrEmailUsername(likeKeyword);

        // 그룹명으로 그룹 멤버 검색
        List<Member> membersByGroupName = groupMemberRepository
                .findMembersByGroupNameContaining(likeKeyword);

        Set<Member> combinedMembers = new HashSet<>(membersByNicknameOrEmail);
        combinedMembers.addAll(membersByGroupName);

        List<MemberSearchResponse> memberResponses = combinedMembers.stream()
                .filter(m -> !m.getId().equals(currentMemberId))
                .map(MemberSearchResponse::of)
                .toList();
        // 그룹 자체 (파생 쿼리는 Spring Data가 LIKE 이스케이프를 처리)
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

        List<Friendship> friendships = friendshipRepository.findAllByMemberAndStatus(member, FriendshipStatus.ACCEPTED);

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
        String refreshToken = authTokenService.genRefreshToken(member, 60 * 60 * 24 * 30); // 30일
        member.setRefreshToken(refreshToken);
        // 강제로 DB반영
        memberRepository.saveAndFlush(member);
        return refreshToken;
    }

    private static String escapeLike(String keyword) {
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    // 로그아웃: refresh 토큰 또는 인증된 회원 기준으로 저장된 refresh 토큰 제거
    @Transactional
    public void clearRefreshToken(String refreshToken, Long memberId) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            memberRepository.findByRefreshToken(refreshToken).ifPresent(m -> m.setRefreshToken(null));
        }
        if (memberId != null) {
            memberRepository.findById(memberId).ifPresent(m -> m.setRefreshToken(null));
        }
    }

    public int getAiDailyLimit() {
        return aiDailyLimit;
    }

    private String aiLimitMessage() {
        return "AI 추천은 하루 " + aiDailyLimit + "회까지만 사용할 수 있습니다.";
    }

    // AI 추천 잔여 횟수 확인 (일 한도 제한, 차감은 하지 않음)
    public void checkAiUsageAvailable(Member member) {
        if (LocalDate.now().equals(member.getAiUsageDate()) && member.getAiUsageCount() >= aiDailyLimit) {
            throw new GlobalException("429-1", aiLimitMessage());
        }
    }

    // AI 추천 성공 후 횟수 차감 (날짜 바뀌면 초기화, 동시 요청은 조건부 UPDATE로 한도 보장)
    @Transactional
    public void incrementAiUsage(Long memberId) {
        if (memberRepository.incrementAiUsage(memberId, LocalDate.now(), aiDailyLimit) == 0) {
            throw new GlobalException("429-1", aiLimitMessage());
        }
    }

    // 소셜 로그인: 기존 회원 조회 or 신규 생성
    @Transactional
    public SocialLoginResult findOrCreateSocialMember(
            MemberProvider provider, String providerId,
            String email, boolean emailVerified, String nickname, String profileImage,
            boolean createIfAbsent) {

        // 1. 소셜 계정 테이블로 조회
        Optional<MemberSocialAccount> account = memberSocialAccountRepository.findByProviderAndProviderId(provider, providerId);
        if (account.isPresent()) {
            return new SocialLoginResult(account.get().getMember(), false);
        }

        // 2. 기존 members.provider + providerId로 조회
        Optional<Member> existing = memberRepository.findByProviderAndProviderId(provider, providerId);
        if (existing.isPresent()) {
            ensureSocialAccount(existing.get(), provider, providerId);
            return new SocialLoginResult(existing.get(), false);
        }

        // 3. 동일 이메일의 계정이 있으면, 제공자가 이메일을 인증한 경우에만 provider 연결 후 반환
        if (email != null) {
            Optional<Member> localMember = memberRepository.findByEmail(email);
            if (localMember.isPresent()) {
                if (!emailVerified) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error(SOCIAL_EMAIL_NOT_VERIFIED), "소셜 계정의 이메일이 인증되지 않았습니다.");
                }
                Member m = localMember.get();
                m.setProvider(provider);
                m.setProviderId(providerId);
                m.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                m.setRefreshToken(null);
                Member saved = memberRepository.save(m);
                ensureSocialAccount(saved, provider, providerId);
                return new SocialLoginResult(saved, false);
            }
        }

        if (!createIfAbsent) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(SOCIAL_ACCOUNT_NOT_FOUND), "존재하지 않는 계정입니다.");
        }

        // 4. 신규 소셜 회원 생성
        String resolvedNickname = limitNickname(nickname);
        if (resolvedNickname == null) {
            resolvedNickname = limitNickname(email != null ? email.split("@")[0] : null);
        }
        if (resolvedNickname == null) {
            resolvedNickname = "user";
        }
        Member newMember = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .nickname(resolvedNickname)
                .profileImage(profileImage)
                .provider(provider)
                .providerId(providerId)
                .profileCompleted(false)
                .build();
        Member saved = memberRepository.save(newMember);
        ensureSocialAccount(saved, provider, providerId);
        return new SocialLoginResult(saved, true);
    }

    // 계정 연동: 이미 같은 회원에게 연결되어 있으면 아무것도 하지 않음
    @Transactional
    public Member linkSocialAccount(Long memberId, MemberProvider provider, String providerId) {
        Member member = validateSocialLink(memberId, provider, providerId);
        ensureSocialAccount(member, provider, providerId);
        return member;
    }

    // 연동 제한 검사만 하고 저장하지 않음 (앱 연동은 확정 API에서 저장)
    public Member validateSocialLink(Long memberId, MemberProvider provider, String providerId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error(SOCIAL_ACCOUNT_NOT_FOUND), "존재하지 않는 계정입니다."));

        Optional<MemberSocialAccount> account = memberSocialAccountRepository.findByProviderAndProviderId(provider, providerId);
        if (account.isPresent()) {
            if (!account.get().getMember().getId().equals(memberId)) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(SOCIAL_ALREADY_LINKED), "다른 회원에게 연결된 소셜 계정입니다.");
            }
            return member;
        }

        boolean usedByOther = memberRepository.findByProviderAndProviderId(provider, providerId)
                .filter(owner -> !owner.getId().equals(memberId))
                .isPresent();
        if (usedByOther) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(SOCIAL_ALREADY_LINKED), "다른 회원에게 연결된 소셜 계정입니다.");
        }

        if (memberSocialAccountRepository.existsByMemberAndProvider(member, provider)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(SOCIAL_PROVIDER_ALREADY_LINKED), "같은 소셜 서비스의 다른 계정이 이미 연결되어 있습니다.");
        }
        return member;
    }

    private void ensureSocialAccount(Member member, MemberProvider provider, String providerId) {
        if (memberSocialAccountRepository.existsByMemberAndProvider(member, provider)) {
            return;
        }
        memberSocialAccountRepository.save(MemberSocialAccount.builder()
                .member(member)
                .provider(provider)
                .providerId(providerId)
                .build());
    }

    private String limitNickname(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() <= NICKNAME_MAX_LENGTH) {
            return value;
        }
        int end = NICKNAME_MAX_LENGTH;
        if (Character.isHighSurrogate(value.charAt(end - 1)) && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        String cut = value.substring(0, end).stripTrailing();
        return cut.isEmpty() ? null : cut;
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
            makeFriendship(jjang, kju);

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
            quoteTagRepository.save(new QuoteTag(q2, guest));
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
