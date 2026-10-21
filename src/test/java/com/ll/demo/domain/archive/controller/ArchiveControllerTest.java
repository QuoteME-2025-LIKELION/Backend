package com.ll.demo.domain.archive.controller;

import com.ll.demo.global.exceptionHandlers.GlobalExceptionHandler;
import com.ll.demo.standard.rq.Rq;
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
import java.time.YearMonth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        .setControllerAdvice(new GlobalExceptionHandler(mock(Rq.class)))
        .setMessageConverters(jsonConverter())
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

        when(quoteService.findMyQuotes(memberId, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/archives/me")
                        .with(authenticatedAs(securityUser)))
                .andExpect(status().isOk());

        verify(quoteService).findMyQuotes(eq(memberId), isNull());
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
    private static MappingJackson2HttpMessageConverter jsonConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MappingJackson2HttpMessageConverter(objectMapper);
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

        @Test
    void getMyQuotes_withMonth_passesYearMonthToService() throws Exception {
        Long memberId = 42L;
        SecurityUser securityUser = createSecurityUser(memberId);
        YearMonth month = YearMonth.of(2026, 9);

        when(quoteService.findMyQuotes(memberId, month)).thenReturn(List.of());

        mockMvc.perform(get("/api/archives/me")
                        .param("month", "2026-09")
                        .with(authenticatedAs(securityUser)))
                .andExpect(status().isOk());

        verify(quoteService).findMyQuotes(eq(memberId), eq(month));
    }

    @Test
    void getMyQuotes_withInvalidMonth_returns400() throws Exception {
        SecurityUser securityUser = createSecurityUser(42L);

        mockMvc.perform(get("/api/archives/me")
                        .param("month", "2026-9")
                        .with(authenticatedAs(securityUser)))
                .andExpect(status().isBadRequest());

        verify(quoteService, never()).findMyQuotes(any(), any());
    }

    @Test
    void getMyQuoteDates_withMonth_returnsDates() throws Exception {
        Long memberId = 42L;
        SecurityUser securityUser = createSecurityUser(memberId);

        when(quoteService.findMyQuoteDates(memberId, YearMonth.of(2026, 9)))
                .thenReturn(List.of(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 3)));

        mockMvc.perform(get("/api/archives/me/dates")
                        .param("month", "2026-09")
                        .with(authenticatedAs(securityUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("2026-09-20"))
                .andExpect(jsonPath("$[1]").value("2026-09-03"));
    }

    @Test
    void getMyQuoteDates_withoutMonth_returns400() throws Exception {
        mockMvc.perform(get("/api/archives/me/dates")
                        .with(authenticatedAs(createSecurityUser(42L))))
                .andExpect(status().isBadRequest());
    }
}
