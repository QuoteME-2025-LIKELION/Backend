package com.ll.demo.domain.profile.profile.dto;

import com.ll.demo.global.validation.BirthYearRange;
import com.ll.demo.global.validation.MaxChars;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 첫 프로필 설정: 닉네임·출생년도 필수, 소개 선택 (검증은 기존 프로필·계정 수정과 같음)
public record ProfileSetupRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @MaxChars(value = 10, message = "닉네임은 10자 이내여야 합니다.")
        String nickname,

        @NotBlank(message = "출생년도는 필수 입력 항목입니다.")
        @Size(min = 4, max = 4, message = "출생년도는 4자리로 입력해주세요.")
        @Pattern(regexp = "\\d{4}", message = "출생년도는 숫자 4자리로 입력해주세요.")
        @BirthYearRange
        String birthYear,

        @MaxChars(value = 20, message = "자기소개는 20자 이내로 작성해주세요.")
        String introduction
) {}
