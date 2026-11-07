package com.ll.demo.global.notion;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestNotionClientTest {

    private MockRestServiceServer server;
    private RestNotionClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestNotionClient(builder, "test-token");
    }

    @Test
    @DisplayName("DB 조회: 인증·버전 헤더, 게시 체크 필터, start_cursor 전달")
    void queryDatabase_sendsHeadersAndFilter() {
        server.expect(once(), requestTo("https://api.notion.com/v1/databases/db-1/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(header("Notion-Version", "2022-06-28"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.filter.property").value("게시"))
                .andExpect(jsonPath("$.filter.checkbox.equals").value(true))
                .andExpect(jsonPath("$.start_cursor").value("cursor-2"))
                .andRespond(withSuccess("{\"results\":[],\"has_more\":false}", MediaType.APPLICATION_JSON));

        JsonNode response = client.queryDatabase("db-1", "게시", "cursor-2");

        assertThat(response.path("has_more").asBoolean()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("블록 조회: start_cursor를 쿼리 파라미터로 전달")
    void blockChildren_paginates() {
        server.expect(once(), requestTo("https://api.notion.com/v1/blocks/page-1/children?page_size=100&start_cursor=c2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"results\":[],\"has_more\":false}", MediaType.APPLICATION_JSON));

        client.getBlockChildren("page-1", "c2");

        server.verify();
    }

    @Test
    @DisplayName("429 → Retry-After 후 1회 재시도")
    void retriesOnceOn429() {
        HttpHeaders retryAfter = new HttpHeaders();
        retryAfter.set(HttpHeaders.RETRY_AFTER, "0");
        server.expect(once(), requestTo("https://api.notion.com/v1/blocks/page-1/children?page_size=100"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter));
        server.expect(once(), requestTo("https://api.notion.com/v1/blocks/page-1/children?page_size=100"))
                .andRespond(withSuccess("{\"results\":[],\"has_more\":false}", MediaType.APPLICATION_JSON));

        assertThat(client.getBlockChildren("page-1", null).path("results").isArray()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("재시도도 429면 예외")
    void failsAfterSecond429() {
        HttpHeaders retryAfter = new HttpHeaders();
        retryAfter.set(HttpHeaders.RETRY_AFTER, "0");
        server.expect(once(), requestTo("https://api.notion.com/v1/blocks/page-1/children?page_size=100"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter));
        server.expect(once(), requestTo("https://api.notion.com/v1/blocks/page-1/children?page_size=100"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter));

        assertThatThrownBy(() -> client.getBlockChildren("page-1", null)).isInstanceOf(HttpClientErrorException.class);
    }
}
