package com.ll.demo.domain.quote.controller;

import com.ll.demo.domain.quote.dto.AiSummaryReq;
import com.ll.demo.domain.quote.dto.QuoteCreateRequest;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.gemini.GeminiService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final GeminiService geminiService;
    private final QuoteService quoteService;

    /**
     * 글 작성 (최종 저장) API
     * [POST] /api/quotes
     * JWT 토큰이 필요합니다.
     */
    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(
            @RequestBody QuoteCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        // 1. 토큰에서 사용자 ID 추출 (프로젝트 설정에 따라 user.getUsername() 형식이 다를 수 있음)
        Long authorId = Long.valueOf(user.getUsername());

        // 2. Service 호출
        // ★ 중요: Service의 createQuote(Long authorId, String content) 순서에 맞춰서 값을 넘겨줍니다.
        QuoteResponse response = quoteService.createQuote(authorId, request.getContent());

        // 3. 결과 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarizeQuote(@RequestBody AiSummaryReq req) {
        String summary = geminiService.summarize(req.content());
        return ResponseEntity.ok(Map.of("summary", summary));
    }
}