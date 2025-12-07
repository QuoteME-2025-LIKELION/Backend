package com.ll.demo.domain.group.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupRequest(
        @NotBlank @Size(max = 10, message = "그룹명은 10자 이하로 입력해주세요") String name,
        @Size(max = 20, message = "좌우명은 20자 이하로 입력해주세요.") String motto
) { }