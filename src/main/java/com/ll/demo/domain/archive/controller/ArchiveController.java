package com.ll.demo.domain.archive.controller;

import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.security.SecurityUser;
import java.time.LocalDate;
import java.util.List;
import java.time.YearMonth;
import java.time.DateTimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final QuoteService quoteService;

    // 날짜별 내 명언 조회 (아카이브 캘린더)
    @GetMapping
    public ResponseEntity<List<QuoteResponse>> getMyQuotesByDate(
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @AuthenticationPrincipal SecurityUser user
    ) {
        List<QuoteResponse> result = quoteService.findMyQuotesByDate(user.getMember().getId(), date);
        return ResponseEntity.ok(result);
    }

    // 내가 작성한 명언 목록 (originalContent + summary 병기), month=yyyy-MM 지정 시 해당 월만
    @GetMapping("/me")
    public List<QuoteResponse> getMyQuotes(
            @RequestParam(value = "month", required = false) String month,
            @AuthenticationPrincipal SecurityUser user
    ) {
        return quoteService.findMyQuotes(user.getMember().getId(), parseMonth(month));
    }

    // 해당 월에 내 명언이 있는 날짜 목록 (달력 표시용)
    @GetMapping("/me/dates")
    public List<LocalDate> getMyQuoteDates(
            @RequestParam(value = "month", required = false) String month,
            @AuthenticationPrincipal SecurityUser user
    ) {
        YearMonth yearMonth = parseMonth(month);
        if (yearMonth == null) {
            throw new GlobalException("400-1", "month 파라미터가 필요합니다. (yyyy-MM)");
        }
        return quoteService.findMyQuoteDates(user.getMember().getId(), yearMonth);
    }

    // 내가 좋아요한 명언 목록
    @GetMapping("/likes")
    public ResponseEntity<List<QuoteResponse>> getLikedQuotes(@AuthenticationPrincipal SecurityUser user) {
        List<QuoteResponse> result = quoteService.findLikedQuotes(user.getMember().getId());
        return ResponseEntity.ok(result);
    }

    // 내가 북마크한 명언 목록
    @GetMapping("/bookmarks")
    public ResponseEntity<List<QuoteResponse>> getBookmarkedQuotes(@AuthenticationPrincipal SecurityUser user) {
        List<QuoteResponse> result = quoteService.findBookmarkedQuotes(user.getMember().getId());
        return ResponseEntity.ok(result);
    }

    private YearMonth parseMonth(String month) {
        if (month == null) {
            return null;
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeException e) {
            throw new GlobalException("400-1", "month는 yyyy-MM 형식이어야 합니다.");
        }
    }
}
