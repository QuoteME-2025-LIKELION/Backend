package com.ll.demo.domain.archive.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.service.QuoteService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArchiveControllerTest {

    private final QuoteService quoteService = mock(QuoteService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ArchiveController controller = new ArchiveController(quoteService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyQuotesByDate_withAuthenticatedSecurityUser_passesMemberIdAndDateToService() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 20);
        Long memberId = 42L;
        SecurityUser securityUser = createSecurityUser(memberId);

        when(quoteService.findMyQuotesByDate(memberId, date)).thenReturn(List.of());

        mockMvc.perform(get("/api/archives")
                        .param("date", date.toString())
                        .with(authenticatedAs(securityUser)))
                .andExpect(status().isOk());

        verify(quoteService).findMyQuotesByDate(eq(memberId), eq(date));
    }

    @Test
    void getMyQuotes_withAuthenticatedSecurityUser_passesMemberIdToService() throws Exception {
        Long memberId = 42L;
        SecurityUser securityUser = createSecurityUser(memberId);

        when(quoteService.findMyQuotes(memberId)).thenReturn(List.of());

        mockMvc.perform(get("/api/archives/me")
                        .with(authenticatedAs(securityUser)))
                .andExpect(status().isOk());

        verify(quoteService).findMyQuotes(eq(memberId));
    }

    @Test
    void getLikedQuotes_withAuthenticatedSecurityUser_passesMemberIdToService() throws Exception {
        Long memberId = 42L;
        SecurityUser securityUser = createSecurityUser(memberId);

        when(quoteService.findLikedQuotes(memberId)).thenReturn(List.of());

        mockMvc.perform(get("/api/archives/likes")
                        .with(authenticatedAs(securityUser)))
                .andExpect(status().isOk());

        verify(quoteService).findLikedQuotes(eq(memberId));
    }

    @Test
    void getBookmarkedQuotes_withAuthenticatedSecurityUser_passesMemberIdToService() throws Exception {
        Long memberId = 42L;
        SecurityUser securityUser = createSecurityUser(memberId);

        when(quoteService.findBookmarkedQuotes(memberId)).thenReturn(List.of());

        mockMvc.perform(get("/api/archives/bookmarks")
                        .with(authenticatedAs(securityUser)))
                .andExpect(status().isOk());

        verify(quoteService).findBookmarkedQuotes(eq(memberId));
    }

    private static SecurityUser createSecurityUser(Long memberId) {
        Member member = Member.builder()
                .email("archive-user@example.com")
                .nickname("아카이브 사용자")
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);

        return new SecurityUser(
                member,
                member.getUsername(),
                "",
                member.getAuthorities()
        );
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
}
