package com.ll.demo.domain.group.group.dto;

import com.ll.demo.global.validation.MaxChars;

public record MottoRequest(
        @MaxChars(value = 20, message = "그룹 메시지는 20자 이하로 입력해주세요.") String motto
) {}