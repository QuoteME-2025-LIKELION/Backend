package com.ll.demo.domain.push.dto;

import com.ll.demo.domain.push.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushTokenRequest(
        @NotBlank @Size(max = 512) String token,
        @NotNull DevicePlatform platform
) { }
