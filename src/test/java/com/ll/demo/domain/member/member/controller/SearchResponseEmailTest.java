package com.ll.demo.domain.member.member.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.security.AuthTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SearchResponseEmailTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AuthTokenService authTokenService;

    @Test
    @DisplayName("이메일 앞부분으로 찾을 수 있지만 응답에 email 키는 없다")
    void searchByEmailPrefix_hidesEmail() throws Exception {
        Member me = memberRepository.save(Member.builder().email("me@test.com").nickname("나").password("password123").build());
        memberRepository.save(Member.builder().email("findme@test.com").nickname("찾는사람").password("password123").build());

        mvc.perform(get("/api/settings/search").param("keyword", "findme")
                        .header("Authorization", "Bearer " + authTokenService.genToken(me, 600)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0].nickname").value("찾는사람"))
                .andExpect(jsonPath("$.members[0].email").doesNotExist());
    }
}
