package com.ll.demo.domain.appversion.controller;

import com.ll.demo.domain.appversion.dto.AppVersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AppVersionController {

    // APP_VERSION, APP_RELEASE_NOTE 환경변수로 덮어쓸 수 있음
    @Value("${app.version:1.0.0}")
    private String version;

    @Value("${app.release-note:버전 1.0.0 업데이트 내용}")
    private String releaseNote;

    @GetMapping("/app-version")
    public ResponseEntity<AppVersionResponse> getAppVersion() {
        AppVersionResponse response = new AppVersionResponse(version, releaseNote);

        return ResponseEntity.ok(response);
    }
}
