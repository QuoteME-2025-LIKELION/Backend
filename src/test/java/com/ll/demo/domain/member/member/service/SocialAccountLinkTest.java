package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.member.member.dto.SocialLoginResult;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.entity.MemberSocialAccount;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.repository.MemberSocialAccountRepository;
import com.ll.demo.domain.member.member.type.MemberProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SocialAccountLinkTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberSocialAccountRepository socialAccountRepository;
    @Autowired private MemberSocialAccountBackfillRunner backfillRunner;

    private Member me;
    private Member other;

    @BeforeEach
    void setUp() {
        me = member("me", MemberProvider.GOOGLE, "g-me");
        other = member("other", MemberProvider.LOCAL, null);
    }

    private Member member(String name, MemberProvider provider, String providerId) {
        return memberRepository.save(Member.builder().email(name + "@test.com").nickname(name).password("password123")
                .provider(provider).providerId(providerId).build());
    }

    private void assertError(String code, Executable executable) {
        OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class, executable);
        assertThat(ex.getError().getErrorCode()).isEqualTo(code);
    }

    private long rows(Member member) {
        return socialAccountRepository.findAllByMemberOrderById(member).size();
    }

    private SocialLoginResult login(MemberProvider provider, String providerId, String email, boolean createIfAbsent) {
        return memberService.findOrCreateSocialMember(provider, providerId, email, true, "이름", null, createIfAbsent);
    }

    @Test
    @DisplayName("신규 연동: 행이 생기고 members.provider는 바뀌지 않는다")
    void link_new() {
        Member linked = memberService.linkSocialAccount(me.getId(), MemberProvider.KAKAO, "k-1");

        assertThat(linked.getId()).isEqualTo(me.getId());
        assertThat(rows(me)).isEqualTo(1);
        assertThat(memberRepository.findById(me.getId()).orElseThrow().getProvider()).isEqualTo(MemberProvider.GOOGLE);
    }

    @Test
    @DisplayName("같은 회원 재연동: 변화 없음")
    void link_same_noChange() {
        memberService.linkSocialAccount(me.getId(), MemberProvider.KAKAO, "k-1");

        memberService.linkSocialAccount(me.getId(), MemberProvider.KAKAO, "k-1");

        assertThat(rows(me)).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 회원에게 연결된 계정: already_linked")
    void link_ownedByOther() {
        memberService.linkSocialAccount(other.getId(), MemberProvider.KAKAO, "k-1");

        assertError("already_linked", () -> memberService.linkSocialAccount(me.getId(), MemberProvider.KAKAO, "k-1"));
    }

    @Test
    @DisplayName("이행 전 members 컬럼으로만 남은 타인 계정도 already_linked")
    void link_ownedByOtherLegacyColumn() {
        assertError("already_linked", () -> memberService.linkSocialAccount(other.getId(), MemberProvider.GOOGLE, "g-me"));
    }

    @Test
    @DisplayName("같은 provider를 다른 ID로 이미 연결: provider_already_linked")
    void link_sameProviderDifferentId() {
        memberService.linkSocialAccount(me.getId(), MemberProvider.KAKAO, "k-1");

        assertError("provider_already_linked", () -> memberService.linkSocialAccount(me.getId(), MemberProvider.KAKAO, "k-2"));
    }

    @Test
    @DisplayName("연동 대상 회원이 없으면 account_not_found")
    void link_memberMissing() {
        assertError("account_not_found", () -> memberService.linkSocialAccount(999999L, MemberProvider.KAKAO, "k-1"));
    }

    @Test
    @DisplayName("연동 후 카카오로 로그인하면 같은 회원 (login 모드)")
    void linkedProvider_logsInSameMember() {
        memberService.linkSocialAccount(me.getId(), MemberProvider.KAKAO, "k-1");

        SocialLoginResult result = login(MemberProvider.KAKAO, "k-1", "different@kakao.com", false);

        assertThat(result.member().getId()).isEqualTo(me.getId());
        assertThat(result.newUser()).isFalse();
    }

    @Test
    @DisplayName("조회 2단계: members 컬럼으로 찾으면 소셜 계정 행을 만든다")
    void login_legacyColumn_createsRow() {
        SocialLoginResult result = login(MemberProvider.GOOGLE, "g-me", "me@test.com", false);

        assertThat(result.member().getId()).isEqualTo(me.getId());
        assertThat(socialAccountRepository.findByProviderAndProviderId(MemberProvider.GOOGLE, "g-me")).isPresent();
    }

    @Test
    @DisplayName("조회 3단계: 같은 이메일 자동 연결은 members 컬럼 갱신과 행 생성")
    void login_sameEmail_createsRow() {
        SocialLoginResult result = login(MemberProvider.KAKAO, "k-9", "other@test.com", false);

        assertThat(result.member().getId()).isEqualTo(other.getId());
        assertThat(memberRepository.findById(other.getId()).orElseThrow().getProviderId()).isEqualTo("k-9");
        assertThat(socialAccountRepository.findByProviderAndProviderId(MemberProvider.KAKAO, "k-9")).isPresent();
    }

    @Test
    @DisplayName("조회 4단계: 신규 생성 시 행도 함께 만든다")
    void login_new_createsRow() {
        SocialLoginResult result = login(MemberProvider.KAKAO, "k-new", "new@test.com", true);

        assertThat(result.newUser()).isTrue();
        assertThat(socialAccountRepository.findByProviderAndProviderId(MemberProvider.KAKAO, "k-new").orElseThrow()
                .getMember().getId()).isEqualTo(result.member().getId());
    }

    @Test
    @DisplayName("이행은 두 번 실행해도 행이 중복되지 않고 LOCAL·providerId 없는 회원은 제외")
    void backfill_isIdempotent() {
        Member noProviderId = member("noid", MemberProvider.KAKAO, null);

        backfillRunner.run(null);
        long first = socialAccountRepository.count();
        backfillRunner.run(null);

        assertThat(socialAccountRepository.count()).isEqualTo(first);
        assertThat(rows(me)).isEqualTo(1);
        assertThat(rows(other)).isZero();
        assertThat(rows(noProviderId)).isZero();
        MemberSocialAccount account = socialAccountRepository.findAllByMemberOrderById(me).get(0);
        assertThat(account.getProviderId()).isEqualTo("g-me");
    }

    @Test
    @DisplayName("같은 소셜 ID를 가진 회원이 여럿이어도 이행이 실패하지 않는다")
    void backfill_duplicateProviderId_skips() {
        Member duplicate = member("dup", MemberProvider.GOOGLE, "g-me");

        backfillRunner.run(null);

        assertThat(rows(me) + rows(duplicate)).isEqualTo(1);
    }
}
