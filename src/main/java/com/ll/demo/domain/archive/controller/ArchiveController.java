package com.ll.demo.domain.archive.controller;

import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.security.SecurityUser;
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

    // 날짜별 명언 조회
    @GetMapping
    public ResponseEntity<List<QuoteResponse>> getQuotesByDate(
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        List<QuoteResponse> result = quoteService.findQuotesByDate(date);
        return ResponseEntity.ok(result);
    }

    // 내가 작성한 명언 목록
    @GetMapping("/me")
    public ResponseEntity<List<QuoteResponse>> getMyQuotes(
            @AuthenticationPrincipal SecurityUser user
    ) {
        List<QuoteResponse> result = quoteService.findMyQuotes(user.getMember().getId());
        return ResponseEntity.ok(result);
    }

    // 내가 좋아요한 명언 목록 조회
    @GetMapping("/likes")
    public ResponseEntity<List<QuoteResponse>> getLikedQuotes(
            @AuthenticationPrincipal SecurityUser user
    ) {
        List<QuoteResponse> result = quoteService.findLikedQuotes(user.getMember().getId());
        return ResponseEntity.ok(result);
    }
}