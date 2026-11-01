package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MemberSearchBlankKeywordTest {

    @Mock private MemberRepository memberRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private GroupRepository groupRepository;
    @InjectMocks private MemberService memberService;

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("빈 검색어는 저장소를 조회하지 않는다")
    void blankKeyword_doesNotQueryRepositories(String keyword) {
        var result = memberService.searchCombined(keyword, 1L);

        assertThat(result.getMembers()).isEmpty();
        assertThat(result.getGroups()).isEmpty();
        verifyNoInteractions(memberRepository, groupMemberRepository, groupRepository);
    }
}
