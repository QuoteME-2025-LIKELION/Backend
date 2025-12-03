package com.ll.demo.domain.quote.dto;

import jakarta.validation.constraints.NotBlank;

// record는 'class'가 아니라서 괄호() 안에 변수를 선언합니다.
public record QuoteCreateRequest(
        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        String originalContent
) {}