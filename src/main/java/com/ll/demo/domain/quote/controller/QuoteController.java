package com.ll.demo.domain.quote.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.dto.AiSummaryReq;
import com.ll.demo.domain.quote.dto.QuoteCreateRequest;
import com.ll.demo.domain.quote.dto.QuoteResponse;
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
        QuoteResponse response = quoteService.createQuote(
                authorId,
                request.content(),        // 명언 (또는 짧은 글)
                request.originalContent() // 원본 일기 (없으면 null 들어옴)
        );

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
    public ResponseEntity<QuoteListDto> getQuoteList(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(value = "date", required = true) // 'date' 쿼리 파라미터를 필수로 받음
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (securityUser == null) {
            // 💡 비로그인 상태일 경우 예외 처리
            throw new RuntimeException("로그인이 필요합니다.");
        }

        // 서비스 계층으로 사용자 정보와 날짜를 넘겨 필터링 및 상세 정보 조회를 요청
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

    // 태그 수정 (PATCH)
    // PATCH /api/quotes/{quoteId}/tags
    @PatchMapping("/{quoteId}/tags")
    public ResponseEntity<Void> updateTags(
            @PathVariable Long quoteId,
            @RequestBody QuoteTagUpdateReq req,
            @AuthenticationPrincipal SecurityUser user
    ) {
        quoteService.updateTags(user.getMember().getId(), quoteId, req.taggedMemberIds());
        return ResponseEntity.ok().build();
    }
}