package com.ll.demo.domain.group.group.service;

import com.ll.demo.domain.group.group.dto.GroupRequest;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.exceptions.GlobalException;
import org.junit.jupiter.api.BeforeEach;
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
class GroupNameDuplicateTest {

    @Autowired private GroupService groupService;
    @Autowired private MemberRepository memberRepository;

    private Member first;
    private Member second;

    @BeforeEach
    void setUp() {
        first = memberRepository.save(Member.builder().email("g1@test.com").nickname("첫째").password("password123").build());
        second = memberRepository.save(Member.builder().email("g2@test.com").nickname("둘째").password("password123").build());
    }

    private void assertConflict(String existing, String requested) {
        groupService.createGroup(first, new GroupRequest(existing, null));

        assertThatThrownBy(() -> groupService.createGroup(second, new GroupRequest(requested, null)))
                .isInstanceOf(GlobalException.class)
                .satisfies(e -> {
                    assertThat(((GlobalException) e).getRsData().getStatusCode()).isEqualTo(409);
                    assertThat(((GlobalException) e).getRsData().getMessage()).isEqualTo("409-1 - 이미 사용 중인 그룹 이름입니다.");
                });
    }

    @Test
    @DisplayName("같은 그룹명 → 409")
    void sameName_409() {
        assertConflict("우리모임", "우리모임");
    }

    @Test
    @DisplayName("앞뒤 공백만 다른 그룹명 → 409")
    void surroundingSpaces_409() {
        assertConflict("우리모임", " 우리모임 ");
    }

    @Test
    @DisplayName("영문 대소문자만 다른 그룹명 → 409")
    void caseOnly_409() {
        assertConflict("Study Club", "study CLUB");
    }

    @Test
    @DisplayName("다른 그룹명은 생성된다")
    void differentName_ok() {
        groupService.createGroup(first, new GroupRequest("우리모임", null));

        assertThat(groupService.createGroup(second, new GroupRequest("너희모임", null)).name()).isEqualTo("너희모임");
    }
}
