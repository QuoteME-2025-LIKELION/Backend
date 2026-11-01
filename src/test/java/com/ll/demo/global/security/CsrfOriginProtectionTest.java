package com.ll.demo.global.security;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CsrfOriginProtectionTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private QuoteLikeRepository quoteLikeRepository;
    @Autowired private AuthTokenService authTokenService;

    private Member liker;
    private Quote quote;
    private String accessToken;

    @BeforeEach
    void setUp() {
        Member author = memberRepository.save(Member.builder().email("author@test.com").nickname("작성자").password("password123").build());
        liker = memberRepository.save(Member.builder().email("liker@test.com").nickname("좋아요").password("password123").build());
        quote = quoteRepository.save(Quote.builder().author(author).content("명언").originalContent("원문").build());
        accessToken = authTokenService.genToken(liker, 600);
    }

    private MockHttpServletRequestBuilder like() {
        return post("/api/quotes/" + quote.getId() + "/like");
    }

    private Cookie cookie() {
        return new Cookie("accessToken", accessToken);
    }

    private long likes() {
        return quoteLikeRepository.count();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example", "null", "not a origin"})
    @DisplayName("허용되지 않은 Origin + 쿠키 인증 → 403, 데이터 변경 없음")
    void disallowedOrigin_403(String origin) throws Exception {
        long before = likes();

        mvc.perform(like().cookie(cookie()).header("Origin", origin))
                .andExpect(status().isForbidden());

        assertThat(likes()).isEqualTo(before);
    }

    @Test
    @DisplayName("허용된 Origin + 쿠키 인증 → 성공")
    void allowedOrigin_ok() throws Exception {
        long before = likes();

        mvc.perform(like().cookie(cookie()).header("Origin", "https://quoteme.shop"))
                .andExpect(status().is2xxSuccessful());

        assertThat(likes()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("Origin 없는 쿠키 요청 → 그대로 통과")
    void noOrigin_ok() throws Exception {
        mvc.perform(like().cookie(cookie())).andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Bearer 요청 → 영향 없음")
    void bearer_ok() throws Exception {
        mvc.perform(like().header("Authorization", "Bearer " + accessToken))
                .andExpect(status().is2xxSuccessful());
    }
}
