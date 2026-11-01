package com.ll.demo.domain.group.group.dto;

import jakarta.validation.constraints.Size;

public record MottoRequest(
        @Size(max = 20, message = "그룹 메시지는 20자 이하로 입력해주세요.") String motto
) {}