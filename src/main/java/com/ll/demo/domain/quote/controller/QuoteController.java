package com.ll.demo.domain.quote.controller;

import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.quote.dto.*;
import com.ll.demo.domain.quote.dto.QuoteSummarizeResponse;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.dto.PagedResponse;
import com.ll.demo.global.gemini.GeminiService;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.SecurityUser;
import com.ll.demo.standard.rq.Rq;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final GeminiService geminiService;
    private final QuoteService quoteService;
    private final MemberService memberService;
    private final Rq rq;

    // 명언 작성
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
                request.summary(),
                request.taggedMemberIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // AI 요약 (일기 → 명언)
    // 일기 내용 검증 후 Gemini 호출
    @PostMapping("/summarize")
    public ResponseEntity<QuoteSummarizeResponse> summarizeQuote(
            @RequestBody AiSummaryReq req,
            @AuthenticationPrincipal SecurityUser user
    ) {
        // 일기 유효성 검사 (15자 이하 / 불량 텍스트 거부)
        quoteService.validateDiaryContent(req.content());
        memberService.checkAndIncrementAiUsage(user.getMember());
        return ResponseEntity.ok(new QuoteSummarizeResponse(geminiService.summarizeThree(req.content())));
    }

    // AI 사용량 조회 (하루 3회 제한)
    // GET /api/quotes/ai-usage
    @GetMapping("/ai-usage")
    public ResponseEntity<Map<String, Object>> getAiUsage(
            @AuthenticationPrincipal SecurityUser user
    ) {
        return ResponseEntity.ok(quoteService.getAiUsageInfo(user.getMember().getId()));
    }

    // 좋아요 등록
    @PostMapping("/{quoteId}/like")
    public ResponseEntity<Void> likeQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        quoteService.likeQuote(securityUser.getMember(), quoteId);
        return ResponseEntity.ok().build();
    }

    // 좋아요 취소
    @DeleteMapping("/{quoteId}/like")
    public ResponseEntity<Void> unlikeQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        quoteService.unlikeQuote(securityUser.getMember(), quoteId);
        return ResponseEntity.ok().build();
    }

    // 피드 목록 조회 (그룹 필터 지원)
    // GET /api/quotes?date=2026-03-01
    // GET /api/quotes?date=2026-03-01&groupId=1  → 그룹 멤버 명언만
    @GetMapping
    public ResponseEntity<QuoteListDto> getQuoteList(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(value = "date", required = true)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "groupId", required = false) Long groupId
    ) {
        if (securityUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(quoteService.getQuoteList(securityUser.getMember(), date, groupId));
    }

    // 태그 요청 전송
    @PostMapping("/{quoteId}/tag-request")
    public ResponseEntity<RsData> requestTagToQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        if (securityUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "401-1. 로그인 인증 정보가 유효하지 않습니다.");
        }
        quoteService.requestTag(securityUser.getMember().getId(), quoteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(RsData.of("201-3", "태그 요청이 명언 작성자에게 전송되었습니다."));
    }

    // 내 태그 요청 상태 확인 (+ 버튼 비활성화 여부용)
    // GET /api/quotes/{quoteId}/my-tag-request → {"status": "PENDING" | "ACCEPTED" | "REJECTED" | "NONE"}
    @GetMapping("/{quoteId}/my-tag-request")
    public ResponseEntity<Map<String, String>> getMyTagRequest(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        String status = quoteService.getMyTagRequestStatus(user.getMember().getId(), quoteId);
        return ResponseEntity.ok(Map.of("status", status));
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
        return ResponseEntity.ok(RsData.of("200", "태그 요청을 수락했습니다."));
    }

    // 태그 요청 거절
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<RsData> rejectTagRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        quoteService.rejectTagRequest(user.getMember().getId(), requestId);
        return ResponseEntity.ok(RsData.of("200", "태그 요청을 거절했습니다."));
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

    // 북마크 추가
    // POST /api/quotes/{quoteId}/bookmark
    @PostMapping("/{quoteId}/bookmark")
    public ResponseEntity<RsData> bookmarkQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        quoteService.bookmarkQuote(user.getMember().getId(), quoteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(RsData.of("201", "북마크에 추가되었습니다."));
    }

    // 북마크 취소
    // DELETE /api/quotes/{quoteId}/bookmark
    @DeleteMapping("/{quoteId}/bookmark")
    public ResponseEntity<RsData> unbookmarkQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        quoteService.unbookmarkQuote(user.getMember().getId(), quoteId);
        return ResponseEntity.ok(RsData.of("200", "북마크가 해제되었습니다."));
    }

    @GetMapping("/feed")
    public RsData<PagedResponse<QuoteDetailResponse>> getFeed(
            @RequestParam LocalDate date,
            @RequestParam(required = false) Long groupId
    ) {
        Long memberId = rq.getMember().getId();
        PagedResponse<QuoteDetailResponse> response = quoteService.getFeed(memberId, date, groupId);
        return RsData.of("200-1", response);
    }

}
