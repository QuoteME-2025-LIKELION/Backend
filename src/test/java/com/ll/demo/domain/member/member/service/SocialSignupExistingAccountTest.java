package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.member.member.dto.SocialLoginResult;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.type.MemberProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SocialSignupExistingAccountTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

    @Test
    @DisplayName("signup 모드(createIfAbsent=true)로 이미 가입된 소셜 계정이 오면 새로 만들지 않고 로그인, newUser=false")
    void signupWithExistingSocialAccount_logsIn() {
        Member existing = memberRepository.save(Member.builder().email("social@test.com").nickname("기존")
                .password("password123").provider(MemberProvider.GOOGLE).providerId("g-1").build());
        long before = memberRepository.count();

        SocialLoginResult result = memberService.findOrCreateSocialMember(
                MemberProvider.GOOGLE, "g-1", "social@test.com", true, "새이름", null, true);

        assertThat(result.member().getId()).isEqualTo(existing.getId());
        assertThat(result.newUser()).isFalse();
        assertThat(result.member().getNickname()).isEqualTo("기존");
        assertThat(memberRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("signup 모드로 인증된 같은 이메일이 오면 기존 계정에 연결, newUser=false")
    void signupWithVerifiedSameEmail_linksExisting() {
        Member existing = memberRepository.save(Member.builder().email("local@test.com").nickname("로컬")
                .password("password123").build());
        long before = memberRepository.count();

        SocialLoginResult result = memberService.findOrCreateSocialMember(
                MemberProvider.KAKAO, "k-1", "local@test.com", true, "카카오", null, true);

        assertThat(result.member().getId()).isEqualTo(existing.getId());
        assertThat(result.newUser()).isFalse();
        assertThat(memberRepository.count()).isEqualTo(before);
    }
}
