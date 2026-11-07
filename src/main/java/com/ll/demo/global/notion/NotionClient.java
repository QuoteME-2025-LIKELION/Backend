package com.ll.demo.global.notion;

import com.fasterxml.jackson.databind.JsonNode;

// 노션 API 호출 (응답 JSON을 그대로 돌려준다. 페이지네이션은 호출하는 쪽에서 next_cursor로 이어 간다)
public interface NotionClient {

    // POST /v1/databases/{id}/query, checkboxProperty가 체크된 페이지만
    JsonNode queryDatabase(String databaseId, String checkboxProperty, String startCursor);

    // GET /v1/blocks/{id}/children
    JsonNode getBlockChildren(String blockId, String startCursor);
}
