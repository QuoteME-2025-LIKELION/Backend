package com.ll.demo.domain.quote.controller;

import com.ll.demo.domain.quote.dto.AiSummaryReq;
import com.ll.demo.domain.quote.dto.QuoteCreateRequest;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.gemini.GeminiService;
import com.ll.demo.global.security.SecurityUser;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.data.domain.Sort;


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
            @AuthenticationPrincipal SecurityUser user // ★ [변경 1] User -> SecurityUser
    ) {
        // ★ [변경 2] 이메일(String)을 파싱하는 게 아니라, 진짜 멤버 ID(Long)를 바로 꺼냅니다.
        Long authorId = user.getMember().getId();

        // 2. Service 호출
        QuoteResponse response = quoteService.createQuote(authorId, request.getContent());

        // 3. 결과 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/summarize")
    public ResponseEntity<Map<String, String>> summarizeQuote(@RequestBody AiSummaryReq req) {
        String summary = geminiService.summarize(req.content());
        return ResponseEntity.ok(Map.of("summary", summary));
    }
    // 좋아요 등록 (POST)
    @PostMapping("/{quoteId}/like")
    public ResponseEntity<Void> likeQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        quoteService.likeQuote(securityUser.getMember(), quoteId);
        return ResponseEntity.ok().build();
    }

    // 좋아요 취소 (DELETE)
    @DeleteMapping("/{quoteId}/like")
    public ResponseEntity<Void> unlikeQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        quoteService.unlikeQuote(securityUser.getMember(), quoteId);
        return ResponseEntity.ok().build();
    }

    // 글 목록 조회 - mj
    @GetMapping
    public ResponseEntity<List<QuoteResponse>> getQuotes() {
        List<QuoteResponse> response = quoteService.getQuoteList();
        return ResponseEntity.ok(response);
    }

    // 태그 요청 등록 - mj
    @PostMapping("/{quoteId}/tag-request")
    public ResponseEntity<Void> tagRequestQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) { Member requester = securityUser.getMember();
        quoteService.tagRequestQuote(requester, quoteId);
        return ResponseEntity.ok().build();
    }

}