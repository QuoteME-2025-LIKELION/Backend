package com.ll.demo.domain.quote.dto;

import jakarta.validation.constraints.NotBlank;

public record AiSummaryReq(
        @NotBlank(message = "내용을 입력해주세요.")
        String content
) {}