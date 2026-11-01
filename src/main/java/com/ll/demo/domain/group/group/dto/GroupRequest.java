package com.ll.demo.domain.group.group.dto;

import com.ll.demo.global.validation.MaxChars;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GroupRequest(
        @NotBlank
        @MaxChars(value = 10, message = "그룹명은 10자 이하로 입력해주세요")
        @Pattern(regexp = GroupRequest.NAME_PATTERN, message = "그룹명은 한글, 영문, 숫자, 공백, 이모지만 사용할 수 있습니다.")
        String name,
        @MaxChars(value = 20, message = "좌우명은 20자 이하로 입력해주세요.") String motto
) {
    // 이모지: Extended_Pictographic + ZWJ + U+FE0F + 피부색 수식자 (#, *는 포함되지 않음)
    static final String NAME_PATTERN = "[\\p{IsHangul}a-zA-Z0-9 \\p{IsExtended_Pictographic}\\u200D\\uFE0F\\x{1F3FB}-\\x{1F3FF}]*";
}