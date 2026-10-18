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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        return memberService.findOrCreateSocialMember(MemberProvider.KAKAO, "1234567890123", email, nickname, null);
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
}