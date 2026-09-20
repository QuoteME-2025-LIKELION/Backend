package com.ll.demo.domain.quote.quote.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.quote.controller.QuoteController;
import com.ll.demo.domain.quote.dto.QuoteDetailResponse;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.dto.PagedResponse;
import com.ll.demo.global.gemini.GeminiService;
import com.ll.demo.global.security.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuoteControllerTest {

    private final QuoteService quoteService = mock(QuoteService.class);
    private final GeminiService geminiService = mock(GeminiService.class);
    private final MemberService memberService = mock(MemberService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        QuoteController controller = new QuoteController(geminiService, quoteService, memberService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getFeed_withAuthenticatedSecurityUser_passesMemberIdToService() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 20);
        Long memberId = 42L;
        Long groupId = 7L;
        Member member = createMember(memberId);
        SecurityUser securityUser = new SecurityUser(
                member,
                member.getUsername(),
                "",
                member.getAuthorities()
        );
        PagedResponse<QuoteDetailResponse> serviceResponse = PagedResponse.empty("empty");

        when(quoteService.getFeed(memberId, date, groupId)).thenReturn(serviceResponse);

        mockMvc.perform(get("/api/quotes/feed")
                        .param("date", date.toString())
                        .param("groupId", groupId.toString())
                        .with(authenticatedAs(securityUser)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("200-1")));

        verify(quoteService).getFeed(eq(memberId), eq(date), eq(groupId));
    }

    @Test
    void getFeed_withoutAuthenticatedSecurityUser_requiresLogin() throws Exception {
        mockMvc.perform(get("/api/quotes/feed")
                        .param("date", "2026-09-20"))
                .andExpect(status().isInternalServerError())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.isEmptyString())));

        verifyNoInteractions(quoteService);
    }

    private static Member createMember(Long memberId) {
        Member member = Member.builder()
                .email("feed-user@example.com")
                .nickname("피드 사용자")
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private static RequestPostProcessor authenticatedAs(SecurityUser securityUser) {
        return request -> {
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    securityUser,
                    null,
                    securityUser.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return request;
        };
    }

    @RestControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler(RuntimeException.class)
        ResponseEntity<String> handleRuntimeException(RuntimeException exception) {
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }
}
