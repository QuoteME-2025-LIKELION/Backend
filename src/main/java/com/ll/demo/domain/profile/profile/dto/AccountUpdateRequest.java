package com.ll.demo.domain.profile.profile.dto;

import com.ll.demo.domain.member.member.type.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountUpdateRequest(
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "올바른 이메일 주소를 입력해주세요.")
        String email,

        @NotBlank(message = "출생년도는 필수 입력 항목입니다.")
        @Size(min = 4, max = 4, message = "출생년도는 4자리로 입력해주세요.")
        String birthYear,

        @NotNull(message = "성별을 선택해주세요.")
        Gender gender
) {}