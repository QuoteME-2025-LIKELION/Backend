package com.ll.demo.domain.notice.repository;

import com.ll.demo.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByImportantDescCreateDateDescIdDesc();

    Optional<Notice> findByNotionPageId(String notionPageId);

    List<Notice> findAllByNotionPageIdIsNotNull();
}
