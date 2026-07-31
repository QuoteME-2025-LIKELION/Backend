package com.ll.demo.domain.notice.service;

import com.ll.demo.domain.notice.dto.NoticeDetailResponse;
import com.ll.demo.domain.notice.dto.NoticeResponse;
import com.ll.demo.domain.notice.entity.Notice;
import com.ll.demo.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    private final NoticeRepository noticeRepository;

    public List<NoticeResponse> findAllNotices() {
        return noticeRepository.findAll().stream()
                .map(NoticeResponse::new)
                .toList();
    }

    public NoticeDetailResponse findNoticeById(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));

        return new NoticeDetailResponse(notice);
    }
}
