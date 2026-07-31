package com.ll.demo.domain.appversion.controller;

import com.ll.demo.domain.appversion.dto.AppVersionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AppVersionController {

    @GetMapping("/app-version")
    public ResponseEntity<AppVersionResponse> getAppVersion() {
        AppVersionResponse response = new AppVersionResponse(
                "1.0.0",
                "버전 1.0.0 업데이트 내용"
        );

        return ResponseEntity.ok(response);
    }
}
