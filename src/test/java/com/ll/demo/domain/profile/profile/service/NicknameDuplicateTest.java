package com.ll.demo.domain.profile.profile.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.profile.profile.dto.ProfileUpdateRequest;
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
class NicknameDuplicateTest {

    @Autowired private ProfileService profileService;
    @Autowired private MemberRepository memberRepository;

    @Test
    @DisplayName("같은 닉네임의 두 회원 모두 저장된다")
    void sameNickname_bothSaved() {
        Member first = memberRepository.saveAndFlush(Member.builder().email("first@test.com").nickname("같은이름").password("password123").build());
        Member second = memberRepository.saveAndFlush(Member.builder().email("second@test.com").nickname("같은이름").password("password123").build());

        assertThat(memberRepository.findById(first.getId())).get().extracting(Member::getNickname).isEqualTo("같은이름");
        assertThat(memberRepository.findById(second.getId())).get().extracting(Member::getNickname).isEqualTo("같은이름");
    }

    @Test
    @DisplayName("이미 쓰는 닉네임으로 수정해도 성공한다")
    void updateToTakenNickname_succeeds() {
        memberRepository.save(Member.builder().email("taken@test.com").nickname("선점").password("password123").build());
        Member me = memberRepository.save(Member.builder().email("me@test.com").nickname("나").password("password123").build());

        profileService.updateProfile(me, new ProfileUpdateRequest("선점", null, null), null);
        memberRepository.flush();

        assertThat(memberRepository.findById(me.getId())).get().extracting(Member::getNickname).isEqualTo("선점");
    }
}
