package com.ll.demo.domain.notice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ll.demo.domain.notice.entity.Notice;
import com.ll.demo.domain.notice.entity.NoticeType;
import com.ll.demo.domain.notice.repository.NoticeRepository;
import com.ll.demo.domain.notice.service.NotionNoticeSyncService;
import com.ll.demo.domain.notice.service.NotionNoticeSyncService.SyncResult;
import com.ll.demo.global.notion.NotionClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotionNoticeSyncTest {

    private static final String PAGE_A = "11111111-1111-1111-1111-111111111111";
    private static final String PAGE_B = "22222222-2222-2222-2222-222222222222";

    @Autowired private NotionNoticeSyncService syncService;
    @Autowired private NoticeRepository noticeRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager em;
    @MockBean private NotionClient notionClient;

    private Notice legacy;

    @BeforeEach
    void setUp() throws IOException {
        noticeRepository.deleteAll();
        legacy = noticeRepository.save(Notice.builder().type(NoticeType.NOTICE).title("직접 등록 공지").content("내용").build());

        given(notionClient.queryDatabase(anyString(), eq("게시"), isNull())).willReturn(json("query-page1.json"));
        given(notionClient.queryDatabase(anyString(), eq("게시"), eq("cursor-2"))).willReturn(json("query-page2.json"));
        given(notionClient.getBlockChildren(eq(PAGE_A), isNull())).willReturn(json("blocks-all-types.json"));
        given(notionClient.getBlockChildren(eq(PAGE_B), isNull())).willReturn(json("blocks-page1.json"));
        given(notionClient.getBlockChildren(eq(PAGE_B), eq("block-cursor-2"))).willReturn(json("blocks-page2.json"));
    }

    private JsonNode json(String name) throws IOException {
        return objectMapper.readTree(new ClassPathResource("notion/" + name).getInputStream());
    }

    private Notice byPage(String pageId) {
        return noticeRepository.findByNotionPageId(pageId).orElseThrow();
    }

    private SyncResult syncAndClear() {
        SyncResult result = syncService.sync();
        em.flush();
        em.clear();
        return result;
    }

    @Test
    @DisplayName("신규 페이지 → 공지 생성 (DB 조회 2쪽, 블록 조회 2쪽 모두 이어 받음)")
    void createsNotices_acrossPages() {
        SyncResult result = syncAndClear();

        assertThat(result).isEqualTo(new SyncResult(2, 0, 0, 0, false));

        Notice a = byPage(PAGE_A);
        assertThat(a.getTitle()).isEqualTo("서비스 점검 안내");
        assertThat(a.getType()).isEqualTo(NoticeType.NOTICE);
        assertThat(a.isImportant()).isTrue();
        assertThat(a.getNotionLastEditedAt()).isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0));

        Notice b = byPage(PAGE_B);
        assertThat(b.getTitle()).isEqualTo("1.2.0 업데이트");
        assertThat(b.getType()).isEqualTo(NoticeType.UPDATE);
        assertThat(b.isImportant()).isFalse();
        assertThat(b.getContent()).isEqualTo("첫 쪽 문단\n둘째 쪽 문단");
    }

    @Test
    @DisplayName("블록 → 텍스트: 제목·문단·목록·인용·구분선 변환, 그 밖의 블록은 무시")
    void blocksToPlainText() {
        syncAndClear();

        assertThat(byPage(PAGE_A).getContent()).isEqualTo(String.join("\n",
                "제목 하나", "제목 둘", "제목 셋", "문단입니다.", "• 글머리 항목", "• 번호 항목", "인용문", "---"));
    }

    @Test
    @DisplayName("last_edited_time이 바뀌면 제목·유형·중요·본문 갱신, 그대로면 본문을 다시 받지 않음")
    void updatesEditedPage() throws IOException {
        syncAndClear();
        Long idBefore = byPage(PAGE_A).getId();

        given(notionClient.queryDatabase(anyString(), eq("게시"), isNull())).willReturn(json("query-page1-edited.json"));
        given(notionClient.getBlockChildren(eq(PAGE_A), isNull())).willReturn(json("blocks-page2.json"));
        SyncResult result = syncAndClear();

        assertThat(result.updated()).isEqualTo(1);
        Notice a = byPage(PAGE_A);
        assertThat(a.getId()).isEqualTo(idBefore);
        assertThat(a.getTitle()).isEqualTo("점검 일정 변경");
        assertThat(a.getType()).isEqualTo(NoticeType.NOTICE);
        assertThat(a.isImportant()).isFalse();
        assertThat(a.getContent()).isEqualTo("둘째 쪽 문단");

        clearInvocations(notionClient);
        SyncResult unchanged = syncAndClear();

        assertThat(unchanged.updated()).isZero();
        verify(notionClient, never()).getBlockChildren(eq(PAGE_A), any());
    }

    @Test
    @DisplayName("게시 해제·삭제로 조회에서 빠진 페이지는 공지 삭제, 직접 등록 공지는 유지")
    void deletesUnpublished_keepsLegacy() throws IOException {
        syncAndClear();

        given(notionClient.queryDatabase(anyString(), eq("게시"), isNull())).willReturn(json("query-empty.json"));
        SyncResult result = syncAndClear();

        assertThat(result.deleted()).isEqualTo(2);
        assertThat(noticeRepository.findAllByNotionPageIdIsNotNull()).isEmpty();
        assertThat(noticeRepository.findById(legacy.getId())).isPresent();
    }

    @Test
    @DisplayName("노션 조회 실패 → 삭제 단계 생략, 기존 공지 유지")
    void queryFailure_keepsNotices() {
        syncAndClear();

        given(notionClient.queryDatabase(anyString(), anyString(), isNull())).willThrow(new ResourceAccessException("notion down"));
        SyncResult result = syncAndClear();

        assertThat(result.queryFailed()).isTrue();
        assertThat(noticeRepository.findAllByNotionPageIdIsNotNull()).hasSize(2);
        assertThat(noticeRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("한 페이지가 실패해도 나머지는 처리하고, 실패 건수를 센다")
    void onePageFails_othersContinue() {
        given(notionClient.getBlockChildren(eq(PAGE_A), isNull())).willThrow(new ResourceAccessException("timeout"));

        SyncResult result = syncAndClear();

        assertThat(result).isEqualTo(new SyncResult(1, 0, 0, 1, false));
        assertThat(noticeRepository.findByNotionPageId(PAGE_A)).isEmpty();
        assertThat(noticeRepository.findByNotionPageId(PAGE_B)).isPresent();
    }

    @Test
    @DisplayName("본문이 컬럼 길이를 넘으면 잘라서 저장")
    void truncatesLongContent() throws IOException {
        ObjectNode longBlocks = (ObjectNode) json("blocks-page2.json");
        ((ObjectNode) longBlocks.path("results").get(0).path("paragraph").path("rich_text").get(0))
                .put("plain_text", "가".repeat(300));
        given(notionClient.getBlockChildren(eq(PAGE_A), isNull())).willReturn(longBlocks);

        syncAndClear();

        assertThat(byPage(PAGE_A).getContent()).hasSize(255);
    }

    @Test
    @DisplayName("동기화가 꺼져 있거나 자격 증명이 없으면 노션을 호출하지 않음")
    void noCredentials_doesNothing() {
        syncService.scheduledSync();

        NotionNoticeSyncService enabledWithoutToken = new NotionNoticeSyncService(notionClient, noticeRepository,
                transactionManager, "", "", true, "제목", "유형", "중요", "게시");
        enabledWithoutToken.scheduledSync();

        NotionNoticeSyncService enabledWithoutDatabase = new NotionNoticeSyncService(notionClient, noticeRepository,
                transactionManager, "secret_dummy", "", true, "제목", "유형", "중요", "게시");
        enabledWithoutDatabase.scheduledSync();

        verifyNoInteractions(notionClient);
        assertThat(noticeRepository.count()).isEqualTo(1);
    }
}
