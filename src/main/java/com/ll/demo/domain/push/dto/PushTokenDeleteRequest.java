package com.ll.demo.domain.push.dto;

import jakarta.validation.constraints.NotBlank;

public record PushTokenDeleteRequest(@NotBlank String token) { }
