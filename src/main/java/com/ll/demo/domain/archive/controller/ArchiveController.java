package com.ll.demo.domain.archive.controller;

import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.security.SecurityUser;
import com.ll.demo.global.security.oauth2.CustomOAuth2User;
import java.time.LocalDate;
import java.util.List;
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
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        List<QuoteResponse> result = quoteService.findMyQuotesByDate(user.getMember().getId(), date);
        return ResponseEntity.ok(result);
    }

    // 내가 작성한 명언 목록 (originalContent + summary 병기)
    @GetMapping("/me")
    public List<QuoteResponse> getMyQuotes(@AuthenticationPrincipal CustomOAuth2User user) {
        return quoteService.findMyQuotes(user.getMember().getId());
    }

    // 내가 좋아요한 명언 목록
    @GetMapping("/likes")
    public ResponseEntity<List<QuoteResponse>> getLikedQuotes(@AuthenticationPrincipal CustomOAuth2User user) {
        List<QuoteResponse> result = quoteService.findLikedQuotes(user.getMember().getId());
        return ResponseEntity.ok(result);
    }

    // 내가 북마크한 명언 목록
    @GetMapping("/bookmarks")
    public ResponseEntity<List<QuoteResponse>> getBookmarkedQuotes(@AuthenticationPrincipal CustomOAuth2User user) {
        List<QuoteResponse> result = quoteService.findBookmarkedQuotes(user.getMember().getId());
        return ResponseEntity.ok(result);
    }
}
