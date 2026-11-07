package com.ll.demo.global.notion;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
public class RestNotionClient implements NotionClient {
    static final String BASE_URL = "https://api.notion.com/v1";
    static final String NOTION_VERSION = "2022-06-28";
    private static final int PAGE_SIZE = 100;
    private static final long MAX_RETRY_AFTER_SECONDS = 60;

    private final RestClient restClient;

    @Autowired
    public RestNotionClient(@Value("${custom.notion.token:}") String token) {
        this(RestClient.builder(), token);
    }

    RestNotionClient(RestClient.Builder builder, String token) {
        this.restClient = builder
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader("Notion-Version", NOTION_VERSION)
                .build();
    }

    @Override
    public JsonNode queryDatabase(String databaseId, String checkboxProperty, String startCursor) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("filter", Map.of("property", checkboxProperty, "checkbox", Map.of("equals", true)));
        body.put("page_size", PAGE_SIZE);
        if (startCursor != null) {
            body.put("start_cursor", startCursor);
        }
        return withRetryOn429(() -> restClient.post()
                .uri("/databases/{id}/query", databaseId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class));
    }

    @Override
    public JsonNode getBlockChildren(String blockId, String startCursor) {
        return withRetryOn429(() -> restClient.get()
                .uri(uri -> {
                    uri.path("/blocks/{id}/children").queryParam("page_size", PAGE_SIZE);
                    if (startCursor != null) {
                        uri.queryParam("start_cursor", startCursor);
                    }
                    return uri.build(blockId);
                })
                .retrieve()
                .body(JsonNode.class));
    }

    // 429면 Retry-After(초)만큼 기다렸다가 1회 재시도
    private JsonNode withRetryOn429(Supplier<JsonNode> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.TOO_MANY_REQUESTS) {
                throw e;
            }
            long waitSeconds = retryAfterSeconds(e.getResponseHeaders());
            log.warn("노션 API 429, {}초 후 1회 재시도", waitSeconds);
            sleep(waitSeconds);
            return call.get();
        }
    }

    private long retryAfterSeconds(HttpHeaders headers) {
        String value = headers != null ? headers.getFirst(HttpHeaders.RETRY_AFTER) : null;
        try {
            return Math.min(MAX_RETRY_AFTER_SECONDS, Math.max(0, Long.parseLong(value == null ? "1" : value.trim())));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("노션 재시도 대기 중 중단됨", e);
        }
    }
}
