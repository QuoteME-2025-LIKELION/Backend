package com.ll.demo.domain.profile.profile.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.profile.profile.dto.ProfileUpdateRequest;
import com.ll.demo.global.exceptions.GlobalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NicknameDuplicateTest {

    @Autowired private ProfileService profileService;
    @Autowired private MemberRepository memberRepository;

    @Test
    @DisplayName("이미 쓰는 닉네임으로 수정 → 409")
    void duplicateNickname_409() {
        memberRepository.save(Member.builder().email("taken@test.com").nickname("선점").password("password123").build());
        Member me = memberRepository.save(Member.builder().email("me@test.com").nickname("나").password("password123").build());

        assertThatThrownBy(() -> profileService.updateProfile(me, new ProfileUpdateRequest("선점", null, null), null))
                .isInstanceOf(GlobalException.class)
                .satisfies(e -> assertThat(((GlobalException) e).getRsData().getStatusCode()).isEqualTo(409));
    }
}
