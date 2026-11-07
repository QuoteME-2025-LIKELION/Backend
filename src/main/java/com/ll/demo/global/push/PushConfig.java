package com.ll.demo.global.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.ByteArrayInputStream;
import java.util.Base64;

@Slf4j
@Configuration
@EnableAsync
public class PushConfig {
    private static final String FIREBASE_APP_NAME = "quoteme-push";

    // 푸시 발송 전용 소규모 풀. 가득 차면 버리고 경고만 남긴다 (원래 요청에 영향 없음)
    @Bean(name = "pushExecutor")
    public ThreadPoolTaskExecutor pushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("push-");
        executor.setRejectedExecutionHandler((task, pool) -> log.warn("푸시 발송 대기열이 가득 차 1건을 버립니다."));
        return executor;
    }

    // FIREBASE_CREDENTIALS_BASE64가 없거나 잘못되면 No-op (앱 기동은 계속)
    @Bean
    public PushSender pushSender(@Value("${custom.firebase.credentials-base64:}") String credentialsBase64) {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS_BASE64가 없어 푸시 알림을 끕니다.");
            return new NoOpPushSender();
        }
        try {
            byte[] json = Base64.getDecoder().decode(credentialsBase64.trim());
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(json)))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(a -> FIREBASE_APP_NAME.equals(a.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, FIREBASE_APP_NAME));
            return new FcmPushSender(FirebaseMessaging.getInstance(app));
        } catch (Exception e) {
            // 예외 메시지에 자격 증명 일부가 섞일 수 있어 유형만 남긴다
            log.warn("Firebase 자격 증명을 읽지 못해 푸시 알림을 끕니다: {}", e.getClass().getSimpleName());
            return new NoOpPushSender();
        }
    }
}
