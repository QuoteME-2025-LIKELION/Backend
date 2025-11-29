package com.ll.demo.domain.quote.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteCreateRequest {
    // 실제 서비스에서는 토큰에서 추출하겠지만, API 테스트를 위해 일단 입력받습니다.
    private Long authorId;
    private String content;
}