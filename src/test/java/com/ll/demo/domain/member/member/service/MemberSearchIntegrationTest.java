package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.group.group.dto.GroupSearchResponse;
import com.ll.demo.domain.member.member.dto.SearchCombinedResponse;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.dto.MemberSearchResponse;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
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
class MemberSearchIntegrationTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;

    private Member me;

    @BeforeEach
    void setUp() {
        me = save("me@test.com", "검색자");
        Member underscore = save("under@test.com", "ab_cd");
        save("plain@test.com", "abXcd");
        Group percent = groupRepository.save(Group.builder().name("50%할인").motto("m").leader(underscore).build());
        groupRepository.save(Group.builder().name("50원모임").motto("m").leader(me).build());
        groupMemberRepository.save(GroupMember.builder().group(percent).member(underscore).build());
    }

    private Member save(String email, String nickname) {
        return memberRepository.save(Member.builder().email(email).nickname(nickname).password("password123").build());
    }

    @Test
    @DisplayName("공백 검색어는 빈 결과")
    void blankKeyword_returnsEmpty() {
        SearchCombinedResponse res = memberService.searchCombined("   ", me.getId());

        assertThat(res.getMembers()).isEmpty();
        assertThat(res.getGroups()).isEmpty();
    }

    @Test
    @DisplayName("_ 는 와일드카드가 아니라 문자 그대로 검색")
    void underscore_isLiteral() {
        SearchCombinedResponse res = memberService.searchCombined("b_c", me.getId());

        assertThat(res.getMembers()).extracting(MemberSearchResponse::getNickname).containsExactly("ab_cd");
    }

    @Test
    @DisplayName("% 는 와일드카드가 아니라 문자 그대로 검색 (그룹명·그룹원)")
    void percent_isLiteral() {
        SearchCombinedResponse res = memberService.searchCombined("50%", me.getId());

        assertThat(res.getGroups()).extracting(GroupSearchResponse::getName).containsExactly("50%할인");
        assertThat(res.getMembers()).extracting(MemberSearchResponse::getNickname).containsExactly("ab_cd");
    }

    @Test
    @DisplayName("일반 부분 검색은 기존과 동일")
    void normalKeyword_unchanged() {
        SearchCombinedResponse res = memberService.searchCombined("cd", me.getId());

        assertThat(res.getMembers()).extracting(MemberSearchResponse::getNickname).containsExactlyInAnyOrder("ab_cd", "abXcd");
    }
}
