package com.ll.demo.domain.profile.profile.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 10, message = "닉네임은 10자 이내여야 합니다.")
        String nickname,

        @Size(max = 30, message = "자기소개는 30자 이내로 작성해주세요.")
        String introduction,

        String profileImage
) {}