package com.ll.demo.domain.profile.profile.dto;

import com.ll.demo.global.validation.MaxChars;

public record ProfileUpdateRequest(
        @MaxChars(value = 10, message = "닉네임은 10자 이내여야 합니다.")
        String nickname,

        @MaxChars(value = 20, message = "자기소개는 20자 이내로 작성해주세요.")
        String introduction,

        String profileImage
) {}