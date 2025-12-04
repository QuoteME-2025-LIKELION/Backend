package com.ll.demo.domain.member.member.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String nickname;
    private String introduction;
    private MultipartFile profileImage;
}