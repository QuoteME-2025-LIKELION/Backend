package com.ll.demo.domain.quote.quote.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.gemini.GeminiService;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiInputLetterValidationTest {

    private static final String NO_LETTER_MESSAGE = "400-7 - 글자가 포함된 내용만 AI 추천을 받을 수 있어요.";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private QuoteService quoteService;
    @Autowired private EntityManager em;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private GeminiService geminiService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder().email("letter@test.com").nickname("글자").password("password123").build());
    }

    private ResultActions summarize(String content) throws Exception {
        return mvc.perform(post("/api/quotes/summarize")
                .header("Authorization", "Bearer " + authTokenService.genToken(member, 600))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", content))));
    }

    private int usageCount() {
        em.flush();
        em.clear();
        return memberRepository.findById(member.getId()).orElseThrow().getAiUsageCount();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀",
            "1234567890123456",
            "!!!???...###@@@$",
            "ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ"
    })
    @DisplayName("일반 문자가 없으면 400-7, Gemini 미호출, 사용 횟수 불변")
    void noLetter_rejected(String content) throws Exception {
        summarize(content)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(NO_LETTER_MESSAGE));

        verify(geminiService, never()).summarizeThree(anyString());
        assertThat(usageCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "오늘은 정말 행복한 하루였다 😀 123!",
            "Today was a really happy day with friends"
    })
    @DisplayName("일반 문자가 있으면 추천을 받는다")
    void withLetter_passes(String content) throws Exception {
        given(geminiService.summarizeThree(anyString())).willReturn(List.of("명언 하나"));

        summarize(content).andExpect(status().isOk());

        assertThat(usageCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"😀🔥✨", "2026", "!?!", "행복 😀 100%"})
    @DisplayName("명언 작성은 이모지·숫자·특수문자를 허용한다")
    void quote_allowsEmojiDigitsSymbols(String content) {
        assertThat(quoteService.createQuote(member.getId(), content, null, null, null).content()).isEqualTo(content);
    }
}
