package com.ll.demo.domain.archive.controller;

import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final QuoteService quoteService;

    @GetMapping
    public ResponseEntity<List<QuoteResponse>> getQuotesByDate(
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        // Service가 DTO를 주니까 바로 리턴하면 됨!
        List<QuoteResponse> result = quoteService.findQuotesByDate(date);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<List<QuoteResponse>> getMyQuotes(
            @AuthenticationPrincipal SecurityUser user
    ) {
        // 여기도 바로 리턴!
        List<QuoteResponse> result = quoteService.findMyQuotes(user.getMember().getId());
        return ResponseEntity.ok(result);
    }
}