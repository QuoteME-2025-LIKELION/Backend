package com.ll.demo.domain.member.member.dto;

import com.ll.demo.global.validation.MaxChars;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ProfileUpdateRequest {
    @MaxChars(value = 10, message = "닉네임은 10자 이내여야 합니다.")
    private String nickname;

    @MaxChars(value = 20, message = "자기소개는 20자 이내로 작성해주세요.")
    private String introduction;

    private MultipartFile profileImage;
}