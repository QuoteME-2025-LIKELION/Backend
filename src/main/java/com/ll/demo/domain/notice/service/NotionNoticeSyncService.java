package com.ll.demo.domain.notice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ll.demo.domain.notice.entity.Notice;
import com.ll.demo.domain.notice.entity.NoticeType;
import com.ll.demo.domain.notice.repository.NoticeRepository;
import com.ll.demo.global.notion.NotionClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 노션 공지 DB → notice 테이블 단방향 동기화 (주기 조회)
@Slf4j
@Service
public class NotionNoticeSyncService {
    // notice.title, notice.content는 VARCHAR(255)
    static final int MAX_COLUMN_LENGTH = 255;

    private final NotionClient notionClient;
    private final NoticeRepository noticeRepository;
    private final TransactionTemplate tx;
    private final String token;
    private final String databaseId;
    private final boolean enabled;
    private final String titleProperty;
    private final String typeProperty;
    private final String importantProperty;
    private final String publishedProperty;

    public NotionNoticeSyncService(
            NotionClient notionClient,
            NoticeRepository noticeRepository,
            PlatformTransactionManager transactionManager,
            @Value("${custom.notion.token:}") String token,
            @Value("${custom.notion.notice-database-id:}") String databaseId,
            @Value("${custom.notion.sync.enabled:false}") boolean enabled,
            @Value("${custom.notion.properties.title:제목}") String titleProperty,
            @Value("${custom.notion.properties.type:유형}") String typeProperty,
            @Value("${custom.notion.properties.important:중요}") String importantProperty,
            @Value("${custom.notion.properties.published:게시}") String publishedProperty
    ) {
        this.notionClient = notionClient;
        this.noticeRepository = noticeRepository;
        this.tx = new TransactionTemplate(transactionManager);
        this.token = token;
        this.databaseId = databaseId;
        this.enabled = enabled;
        this.titleProperty = titleProperty;
        this.typeProperty = typeProperty;
        this.importantProperty = importantProperty;
        this.publishedProperty = publishedProperty;
    }

    public record SyncResult(int created, int updated, int deleted, int failed, boolean queryFailed) { }

    @PostConstruct
    void warnIfMissingCredentials() {
        if (enabled && !hasCredentials()) {
            log.warn("NOTION_TOKEN 또는 NOTION_NOTICE_DATABASE_ID가 없어 노션 공지 동기화를 끕니다.");
        }
    }

    private boolean hasCredentials() {
        return token != null && !token.isBlank() && databaseId != null && !databaseId.isBlank();
    }

    @Scheduled(fixedDelayString = "${custom.notion.sync.interval-ms:600000}")
    public void scheduledSync() {
        if (!enabled || !hasCredentials()) {
            return;
        }
        sync();
    }

    public SyncResult sync() {
        List<JsonNode> pages;
        try {
            pages = fetchPublishedPages();
        } catch (Exception e) {
            // 조회 실패를 "페이지 없음"으로 보고 공지를 지우지 않도록 삭제 단계를 건너뛴다
            log.warn("노션 공지 조회 실패, 이번 동기화를 건너뜁니다: {}", e.toString());
            return new SyncResult(0, 0, 0, 0, true);
        }

        int created = 0, updated = 0, deleted = 0, failed = 0;
        Set<String> seen = new HashSet<>();

        for (JsonNode page : pages) {
            String pageId = page.path("id").asText(null);
            if (pageId == null || page.path("archived").asBoolean(false) || page.path("in_trash").asBoolean(false)) {
                continue;
            }
            seen.add(pageId);
            try {
                Boolean isNew = upsert(pageId, page);
                if (isNew == null) continue;
                if (isNew) created++;
                else updated++;
            } catch (Exception e) {
                failed++;
                log.warn("노션 공지 페이지 동기화 실패 pageId={}: {}", pageId, e.toString());
            }
        }

        for (Notice notice : noticeRepository.findAllByNotionPageIdIsNotNull()) {
            if (seen.contains(notice.getNotionPageId())) continue;
            try {
                tx.executeWithoutResult(status -> noticeRepository.deleteById(notice.getId()));
                deleted++;
            } catch (Exception e) {
                failed++;
                log.warn("노션 공지 삭제 실패 noticeId={}: {}", notice.getId(), e.toString());
            }
        }

        log.info("노션 공지 동기화: 생성 {}, 갱신 {}, 삭제 {}, 실패 {}", created, updated, deleted, failed);
        return new SyncResult(created, updated, deleted, failed, false);
    }

    private List<JsonNode> fetchPublishedPages() {
        List<JsonNode> pages = new ArrayList<>();
        String cursor = null;
        do {
            JsonNode response = notionClient.queryDatabase(databaseId, publishedProperty, cursor);
            if (response == null || !response.path("results").isArray()) {
                throw new IllegalStateException("노션 DB 조회 응답 형식이 올바르지 않습니다.");
            }
            response.path("results").forEach(pages::add);
            cursor = nextCursor(response);
        } while (cursor != null);
        return pages;
    }

    // true: 생성, false: 갱신, null: 변경 없음
    private Boolean upsert(String pageId, JsonNode page) {
        LocalDateTime lastEditedAt = OffsetDateTime.parse(page.path("last_edited_time").asText())
                .withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        Notice existing = noticeRepository.findByNotionPageId(pageId).orElse(null);
        if (existing != null && lastEditedAt.equals(existing.getNotionLastEditedAt())) {
            return null;
        }

        JsonNode properties = page.path("properties");
        NoticeType type = toNoticeType(properties.path(typeProperty).path("select").path("name").asText(""));
        boolean important = properties.path(importantProperty).path("checkbox").asBoolean(false);
        String title = limit(plainText(properties.path(titleProperty).path("title")), "title", pageId);
        String content = limit(fetchContent(pageId), "content", pageId);

        tx.executeWithoutResult(status -> {
            if (existing == null) {
                Notice notice = Notice.builder().type(type).title(title).content(content).important(important)
                        .notionPageId(pageId).notionLastEditedAt(lastEditedAt).build();
                noticeRepository.save(notice);
            } else {
                existing.updateFromNotion(type, title, content, important, lastEditedAt);
                noticeRepository.save(existing);
            }
        });
        return existing == null;
    }

    private NoticeType toNoticeType(String name) {
        return "업데이트".equals(name) ? NoticeType.UPDATE : NoticeType.NOTICE;
    }

    private String fetchContent(String pageId) {
        List<String> lines = new ArrayList<>();
        String cursor = null;
        do {
            JsonNode response = notionClient.getBlockChildren(pageId, cursor);
            for (JsonNode block : response.path("results")) {
                String line = blockToText(block);
                if (line != null) lines.add(line);
            }
            cursor = nextCursor(response);
        } while (cursor != null);
        return String.join("\n", lines);
    }

    // N04: 문단·제목·인용은 그대로, 목록은 "• ", 구분선은 "---", 그 밖의 블록은 무시
    static String blockToText(JsonNode block) {
        String type = block.path("type").asText("");
        return switch (type) {
            case "paragraph", "heading_1", "heading_2", "heading_3", "quote" ->
                    plainText(block.path(type).path("rich_text"));
            case "bulleted_list_item", "numbered_list_item" -> "• " + plainText(block.path(type).path("rich_text"));
            case "divider" -> "---";
            default -> null;
        };
    }

    private static String plainText(JsonNode richText) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : richText) {
            sb.append(part.path("plain_text").asText(""));
        }
        return sb.toString();
    }

    private static String nextCursor(JsonNode response) {
        if (!response.path("has_more").asBoolean(false)) return null;
        String cursor = response.path("next_cursor").asText(null);
        return cursor == null || cursor.isBlank() ? null : cursor;
    }

    private String limit(String value, String field, String pageId) {
        if (value.length() <= MAX_COLUMN_LENGTH) return value;
        int end = MAX_COLUMN_LENGTH;
        if (Character.isHighSurrogate(value.charAt(end - 1))) end--;
        log.warn("노션 공지 {}가 {}자를 넘어 잘라 저장합니다 pageId={}", field, MAX_COLUMN_LENGTH, pageId);
        return value.substring(0, end);
    }
}
