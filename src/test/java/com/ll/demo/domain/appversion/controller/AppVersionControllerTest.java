package com.ll.demo.domain.appversion.controller;

import com.ll.demo.domain.appversion.dto.AppVersionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AppVersionControllerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AppVersionController.class);

    @Test
    @DisplayName("설정이 없으면 기존 문자열을 반환")
    void defaults() {
        runner.run(context -> {
            AppVersionResponse body = context.getBean(AppVersionController.class).getAppVersion().getBody();
            assertThat(body).usingRecursiveComparison().isEqualTo(new AppVersionResponse("1.0.0", "버전 1.0.0 업데이트 내용"));
        });
    }

    @Test
    @DisplayName("app.version, app.release-note로 덮어쓸 수 있다")
    void overridden() {
        runner.withPropertyValues("app.version=2.1.0", "app.release-note=새 기능").run(context -> {
            AppVersionResponse body = context.getBean(AppVersionController.class).getAppVersion().getBody();
            assertThat(body).usingRecursiveComparison().isEqualTo(new AppVersionResponse("2.1.0", "새 기능"));
        });
    }
}
