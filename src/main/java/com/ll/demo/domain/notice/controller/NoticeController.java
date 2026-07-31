package com.ll.demo.domain.notice.controller;

import com.ll.demo.domain.notice.dto.NoticeDetailResponse;
import com.ll.demo.domain.notice.dto.NoticeResponse;
import com.ll.demo.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getNotices() {
        return ResponseEntity.ok(noticeService.findAllNotices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticeDetailResponse> getNoticeById(@PathVariable Long id) {
        return ResponseEntity.ok(noticeService.findNoticeById(id));
    }
}
