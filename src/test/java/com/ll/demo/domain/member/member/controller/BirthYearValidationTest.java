package com.ll.demo.domain.member.member.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.global.security.AuthTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BirthYearValidationTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private AuthTokenService authTokenService;

    private Member member(String birthYear) {
        return memberRepository.save(Member.builder().email("birth@test.com").nickname("출생").password("password123")
                .birthYear(birthYear).build());
    }

    private ResultActions updateAccount(Member member, String birthYear) throws Exception {
        return mvc.perform(put("/api/profile/account")
                .header("Authorization", "Bearer " + authTokenService.genToken(member, 600))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"birth@test.com\",\"birthYear\":\"" + birthYear + "\",\"gender\":\"MALE\"}"));
    }

    private ResultActions join(String birthYear) throws Exception {
        return mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@test.com\",\"password\":\"password123\",\"birthYear\":\"" + birthYear + "\"}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "1899", "12a4", "9999"})
    @DisplayName("계정 수정: 잘못된 출생년도 → 400")
    void updateAccount_invalid_400(String birthYear) throws Exception {
        updateAccount(member("2000"), birthYear).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("계정 수정: 경계값(1900, 올해) → 200")
    void updateAccount_boundaries_200() throws Exception {
        Member member = member("2000");
        updateAccount(member, "1900").andExpect(status().isOk());
        updateAccount(member, String.valueOf(Year.now().getValue())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "1899"})
    @DisplayName("회원 가입: 잘못된 출생년도 → 400")
    void join_invalid_400(String birthYear) throws Exception {
        join(birthYear).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DB에 숫자가 아닌 출생년도가 있는 회원의 명언 목록 → 200, null")
    void nonNumericBirthYearInDb_200() throws Exception {
        Member member = member("abcd");
        quoteRepository.save(Quote.builder().author(member).content("명언").originalContent("원문").build());

        mvc.perform(get("/api/archives/me")
                        .header("Authorization", "Bearer " + authTokenService.genToken(member, 600)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("명언"))
                .andExpect(jsonPath("$[0].authorBirthYear").value(nullValue()));
    }
}
