package com.ll.demo.domain.notice;

import com.ll.demo.domain.notice.dto.NoticeDetailResponse;
import com.ll.demo.domain.notice.dto.NoticeResponse;
import com.ll.demo.domain.notice.entity.Notice;
import com.ll.demo.domain.notice.entity.NoticeType;
import com.ll.demo.domain.notice.repository.NoticeRepository;
import com.ll.demo.domain.notice.service.NoticeService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoticeImportantTest {

    @Autowired private NoticeService noticeService;
    @Autowired private NoticeRepository noticeRepository;
    @Autowired private EntityManager em;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
    }

    private Notice notice(String title, boolean important, int day) {
        Notice notice = noticeRepository.saveAndFlush(Notice.builder().type(NoticeType.NOTICE).title(title).content("내용")
                .important(important).build());
        em.createNativeQuery("update notice set create_date = ?1 where id = ?2")
                .setParameter(1, LocalDateTime.of(2026, 3, day, 12, 0))
                .setParameter(2, notice.getId())
                .executeUpdate();
        em.clear();
        return notice;
    }

    @Test
    @DisplayName("목록은 중요 공지 먼저, 그다음 최신순")
    void list_importantFirstThenLatest() {
        notice("일반 옛날", false, 1);
        notice("중요 옛날", true, 2);
        notice("일반 최신", false, 4);
        notice("중요 최신", true, 3);

        List<NoticeResponse> notices = noticeService.findAllNotices();

        assertThat(notices).extracting(NoticeResponse::title).containsExactly("중요 최신", "중요 옛날", "일반 최신", "일반 옛날");
        assertThat(notices).extracting(NoticeResponse::important).containsExactly(true, true, false, false);
    }

    @Test
    @DisplayName("빌더 기본값은 false, 상세 응답에 important 포함")
    void builderDefault_false() {
        Notice notice = noticeRepository.saveAndFlush(Notice.builder().type(NoticeType.NOTICE).title("제목").content("내용").build());

        NoticeDetailResponse detail = noticeService.findNoticeById(notice.getId());

        assertThat(detail.important()).isFalse();
    }

    @Test
    @DisplayName("important를 지정하지 않은 기존 방식 INSERT도 DB 기본값 false로 저장된다")
    void columnDefault_false() {
        em.createNativeQuery("insert into notice (type, title, content) values ('NOTICE', '기존 행', '내용')").executeUpdate();

        Object important = em.createNativeQuery("select important from notice where title = '기존 행'").getSingleResult();

        assertThat(important).isEqualTo(false);
    }
}
