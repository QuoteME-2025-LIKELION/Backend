package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.domain.member.member.type.MemberProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthTokenService authTokenService;

    @InjectMocks private MemberService memberService;

    @Test
    @DisplayName("회원가입 성공 - 비밀번호 암호화 및 저장 확인")
    void join_Success() {
        // 테스트 데이터
        String email = "a@test.com";
        String rawPw = "1234";
        String encodedPw = "encoded_hash";

        when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPw)).thenReturn(encodedPw);

        RsData<Member> result = memberService.join(email, rawPw, "1995");

        assertThat(result.getData()).isNotNull();
        verify(memberRepository).save(argThat(member ->
                member.getEmail().equals(email) &&
                        member.getPassword().equals(encodedPw)
        ));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복 시 GlobalException 발생")
    void join_Fail_Duplicate() {
        String existingEmail = "old@test.com";
        Member existingMember = Member.builder().email(existingEmail).build();
        when(memberRepository.findByEmail(existingEmail)).thenReturn(Optional.of(existingMember));

        assertThatThrownBy(() -> memberService.join(existingEmail, "pw", "2000"))
                .isInstanceOf(GlobalException.class)
                .hasMessageContaining("이미 존재하는 이메일");
    }

    @Test
    @DisplayName("리프레시 토큰 생성 시 DB 반영 확인")
    void genRefreshToken_Update() {
        Member member = Member.builder().email("test@test.com").build();
        String mockToken = "mock_token";
        when(authTokenService.genToken(any(Member.class), anyInt())).thenReturn(mockToken);

        String token = memberService.genRefreshToken(member);

        assertThat(token).isEqualTo(mockToken);
        verify(memberRepository).saveAndFlush(member);
    }

    private Member signUpSocial(String nickname, String email) {
        lenient().when(memberRepository.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        lenient().when(memberRepository.findByEmail(any())).thenReturn(Optional.empty());
        lenient().when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return memberService.findOrCreateSocialMember(MemberProvider.KAKAO, "1234567890123", email, true, nickname, null, true);
    }

    @Test
    @DisplayName("소셜 가입 - 10자 이하 닉네임은 그대로 저장")
    void socialNickname_short_kept() {
        assertThat(signUpSocial("홍길동", "a@test.com").getNickname()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("소셜 가입 - 11자 이상 닉네임은 10자로 잘라 저장")
    void socialNickname_long_truncated() {
        assertThat(signUpSocial("가나다라마바사아자차카", "a@test.com").getNickname()).isEqualTo("가나다라마바사아자차");
    }

    @Test
    @DisplayName("소셜 가입 - 이모지 중간에서 자르지 않는다")
    void socialNickname_emojiBoundary() {
        assertThat(signUpSocial("가나다라마바사아자\uD83D\uDE00", "a@test.com").getNickname()).isEqualTo("가나다라마바사아자");
    }

    @Test
    @DisplayName("소셜 가입 - 이름이 비면 이메일 앞부분을 10자 이내로 사용")
    void socialNickname_blank_fallsBackToEmail() {
        assertThat(signUpSocial(" ", "abcdefghijklmnop@test.com").getNickname()).isEqualTo("abcdefghij");
    }

    @Test
    @DisplayName("소셜 가입 - 이름과 이메일이 모두 없으면 user")
    void socialNickname_noNameNoEmail_user() {
        assertThat(signUpSocial(null, null).getNickname()).isEqualTo("user");
    }

    private Member localMember(String email) {
        return Member.builder().email(email).build();
    }

    private void assertSocialError(Runnable call, String expectedCode) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
                        e -> assertThat(e.getError().getErrorCode()).isEqualTo(expectedCode));
    }

    @Test
    @DisplayName("소셜 - 이미 연결된 계정은 그대로 반환하고 이메일 조회도 하지 않는다")
    void social_existingProviderAccount_returned() {
        Member linked = localMember("a@test.com");
        when(memberRepository.findByProviderAndProviderId(MemberProvider.GOOGLE, "g-1")).thenReturn(Optional.of(linked));

        Member result = memberService.findOrCreateSocialMember(MemberProvider.GOOGLE, "g-1", "a@test.com", true, "이름", null, false);

        assertThat(result).isSameAs(linked);
        verify(memberRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("소셜 로그인 - 미가입 계정이면 생성하지 않고 account_not_found")
    void social_loginMode_unknownAccount_notCreated() {
        when(memberRepository.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        assertSocialError(() -> memberService.findOrCreateSocialMember(
                MemberProvider.GOOGLE, "g-2", "new@test.com", true, "이름", null, false),
                MemberService.SOCIAL_ACCOUNT_NOT_FOUND);

        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("소셜 가입 - 미가입 계정이면 생성한다")
    void social_signupMode_unknownAccount_created() {
        when(memberRepository.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = memberService.findOrCreateSocialMember(
                MemberProvider.GOOGLE, "g-3", "new@test.com", true, "이름", null, true);

        assertThat(result.getProvider()).isEqualTo(MemberProvider.GOOGLE);
        assertThat(result.getProviderId()).isEqualTo("g-3");
        assertThat(result.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    @DisplayName("소셜 - 인증된 이메일이 기존 LOCAL 계정과 같으면 연결하고 기존 비밀번호와 리프레시 토큰을 무효화한다")
    void social_verifiedEmail_linksExistingAccountAndInvalidatesLocalCredentials() {
        Member local = Member.builder()
                .email("a@test.com")
                .password("old-known-hash")
                .refreshToken("old-refresh")
                .build();
        when(memberRepository.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail("a@test.com")).thenReturn(Optional.of(local));
        when(passwordEncoder.encode(anyString())).thenReturn("rotated-hash");
        when(memberRepository.save(local)).thenReturn(local);

        Member result = memberService.findOrCreateSocialMember(
                MemberProvider.GOOGLE, "g-4", "a@test.com", true, "이름", null, false);

        assertThat(result).isSameAs(local);
        assertThat(local.getProvider()).isEqualTo(MemberProvider.GOOGLE);
        assertThat(local.getProviderId()).isEqualTo("g-4");
        assertThat(local.getPassword()).isEqualTo("rotated-hash");
        assertThat(local.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("소셜 - 인증되지 않은 이메일은 기존 계정에 연결하지 않고 email_not_verified")
    void social_unverifiedEmail_doesNotLink() {
        Member local = localMember("a@test.com");
        when(memberRepository.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail("a@test.com")).thenReturn(Optional.of(local));

        assertSocialError(() -> memberService.findOrCreateSocialMember(
                MemberProvider.KAKAO, "k-1", "a@test.com", false, "이름", null, true),
                MemberService.SOCIAL_EMAIL_NOT_VERIFIED);

        assertThat(local.getProvider()).isEqualTo(MemberProvider.LOCAL);
        assertThat(local.getProviderId()).isNull();
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("소셜 가입 - 인증되지 않은 이메일이라도 겹치는 계정이 없으면 가입된다 (카카오 가짜 이메일)")
    void social_unverifiedEmail_noExistingAccount_created() {
        when(memberRepository.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail("123@kakao.user")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = memberService.findOrCreateSocialMember(
                MemberProvider.KAKAO, "123", "123@kakao.user", false, null, null, true);

        assertThat(result.getProvider()).isEqualTo(MemberProvider.KAKAO);
        assertThat(result.getNickname()).isEqualTo("123");
    }

    @Test
    @DisplayName("이메일 가입 - 소셜 가짜 이메일 도메인(@kakao.user)은 거부한다")
    void join_Fail_PlaceholderDomain() {
        assertThatThrownBy(() -> memberService.join("123@Kakao.User", "pw123456", "1995"))
                .isInstanceOf(GlobalException.class)
                .hasMessageContaining("400-2");

        verify(memberRepository, never()).save(any());
    }
}