package com.ll.demo.domain.quote.controller;

import com.ll.demo.domain.quote.dto.AiSummaryReq;
import com.ll.demo.domain.quote.dto.QuoteCreateRequest;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.dto.QuoteTagRequestResponse;
import com.ll.demo.domain.quote.dto.QuoteTagUpdateReq;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.domain.quote.dto.QuoteListDto;
import com.ll.demo.global.gemini.GeminiService;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.SecurityUser;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final GeminiService geminiService;
    private final QuoteService quoteService;

    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(
            @RequestBody QuoteCreateRequest request,
            @AuthenticationPrincipal SecurityUser user
    ) {
        Long authorId = user.getMember().getId();

        QuoteResponse response = quoteService.createQuote(
                authorId,
                request.content(),
                request.originalContent(),
                request.taggedMemberIds()
        );

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

    // 글 목록 조회
    @GetMapping
    public ResponseEntity<QuoteListDto> getQuoteList(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(value = "date", required = true)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (securityUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(quoteService.getQuoteList(securityUser.getMember(), date));
    }

    // 태그 요청
    @PostMapping("/{quoteId}/tag-request")
    public ResponseEntity<RsData> requestTagToQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        if (securityUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "401-1. 로그인 인증 정보가 유효하지 않습니다."
            );
        }
        Member requester = securityUser.getMember();
        quoteService.requestTagToQuote(quoteId, requester);
        return ResponseEntity.status(HttpStatus.CREATED).body(RsData.of("201-3", "태그 요청이 명언 작성자에게 전송되었습니다."));
    }

    // 태그 수정
    @PatchMapping("/{quoteId}/tags")
    public ResponseEntity<Void> updateTags(
            @PathVariable Long quoteId,
            @RequestBody QuoteTagUpdateReq req,
            @AuthenticationPrincipal SecurityUser user
    ) {
        quoteService.updateTags(user.getMember().getId(), quoteId, req.taggedMemberIds());
        return ResponseEntity.ok().build();
    }

    // 태그 요청 수락
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<RsData> acceptTagRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        quoteService.acceptTagRequest(user.getMember().getId(), requestId);

        return ResponseEntity.ok(
                RsData.of("200", "태그 요청을 수락했습니다.")
        );
    }

    // 태그 요청 거절
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<RsData> rejectTagRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        quoteService.rejectTagRequest(user.getMember().getId(), requestId);

        return ResponseEntity.ok(
                RsData.of("200", "태그 요청을 거절했습니다.")
        );
    }

    // 태그 요청 목록 조회
    @GetMapping("/{quoteId}/requests")
    public ResponseEntity<List<QuoteTagRequestResponse>> getTagRequests(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        List<QuoteTagRequestResponse> response = quoteService.getPendingTagRequests(user.getMember().getId(), quoteId);
        return ResponseEntity.ok(response);
    }
}