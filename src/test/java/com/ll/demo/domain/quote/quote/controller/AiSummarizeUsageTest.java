package com.ll.demo.domain.quote.quote.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.gemini.GeminiService;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
class AiSummarizeUsageTest {

    private static final String BODY = "{\"content\":\"오늘은 친구들과 오랫동안 이야기를 나누며 많은 생각을 한 하루였다\"}";

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private EntityManager em;
    @MockBean private GeminiService geminiService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder().email("ai@test.com").nickname("에이아이").password("password123").build());
    }

    private ResultActions summarize() throws Exception {
        return mvc.perform(post("/api/quotes/summarize")
                .header("Authorization", "Bearer " + authTokenService.genToken(member, 600))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY));
    }

    private Member reload() {
        em.flush();
        em.clear();
        return memberRepository.findById(member.getId()).orElseThrow();
    }

    private void setUsage(int count, LocalDate date) {
        member.setAiUsageCount(count);
        member.setAiUsageDate(date);
        memberRepository.saveAndFlush(member);
    }

    @Test
    @DisplayName("추천 성공 → 횟수 1 증가")
    void success_increments() throws Exception {
        given(geminiService.summarizeThree(anyString())).willReturn(List.of("명언 하나", "명언 둘"));

        summarize().andExpect(status().isOk()).andExpect(jsonPath("$.summaries.length()").value(2));

        Member m = reload();
        assertThat(m.getAiUsageCount()).isEqualTo(1);
        assertThat(m.getAiUsageDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("추천 0개 → 빈 목록 200, 횟수 불변")
    void empty_notCounted() throws Exception {
        given(geminiService.summarizeThree(anyString())).willReturn(List.of());

        summarize().andExpect(status().isOk()).andExpect(jsonPath("$.summaries.length()").value(0));

        assertThat(reload().getAiUsageCount()).isZero();
    }

    @Test
    @DisplayName("Gemini 예외 → 횟수 불변")
    void exception_notCounted() throws Exception {
        given(geminiService.summarizeThree(anyString())).willThrow(new IllegalStateException("gemini down"));

        summarize().andExpect(status().isInternalServerError());

        assertThat(reload().getAiUsageCount()).isZero();
    }

    @Test
    @DisplayName("미처리 예외 응답에 내부 메시지가 노출되지 않음, 키 구조 유지")
    void unhandledException_hidesInternalMessage() throws Exception {
        given(geminiService.summarizeThree(anyString()))
                .willThrow(new IllegalStateException("could not execute statement [select password from member]"));

        String body = summarize()
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.resultCode").value("500-1"))
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.msg").value("서버 오류가 발생했습니다."))
                .andExpect(jsonPath("$.data.path").value("/api/quotes/summarize"))
                .andExpect(jsonPath("$.data.trace").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("select password", "IllegalStateException");
    }

    @Test
    @DisplayName("오늘 3회 사용 → 429-1, Gemini 미호출")
    void limitReached_notCalled() throws Exception {
        setUsage(3, LocalDate.now());

        summarize().andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.resultCode").value("429"));

        verify(geminiService, never()).summarizeThree(anyString());
    }

    @Test
    @DisplayName("날짜가 바뀌면 초기화 후 1부터")
    void newDay_resets() throws Exception {
        setUsage(3, LocalDate.now().minusDays(1));
        given(geminiService.summarizeThree(anyString())).willReturn(List.of("명언 하나"));

        summarize().andExpect(status().isOk());

        Member m = reload();
        assertThat(m.getAiUsageCount()).isEqualTo(1);
        assertThat(m.getAiUsageDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("조건부 UPDATE는 한도를 넘겨 증가시키지 않음")
    void incrementIsConditional() {
        setUsage(3, LocalDate.now());

        int updated = memberRepository.incrementAiUsage(member.getId(), LocalDate.now(), 3);

        assertThat(updated).isZero();
        assertThat(reload().getAiUsageCount()).isEqualTo(3);
    }
}
