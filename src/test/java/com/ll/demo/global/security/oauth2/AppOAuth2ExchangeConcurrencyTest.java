package com.ll.demo.global.security.oauth2;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.exceptions.GlobalException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 커밋되는 트랜잭션으로 동시 교환을 검증 (테스트 트랜잭션 없음)
@SpringBootTest
@ActiveProfiles("test")
class AppOAuth2ExchangeConcurrencyTest {

    @Autowired private AppOAuth2Service appOAuth2Service;
    @Autowired private OAuth2ExchangeCodeRepository exchangeCodeRepository;
    @Autowired private MemberRepository memberRepository;

    private Member member;

    @AfterEach
    void tearDown() {
        exchangeCodeRepository.deleteAll();
        if (member != null) {
            memberRepository.deleteById(member.getId());
        }
    }

    @Test
    @DisplayName("같은 코드로 동시에 두 번 교환하면 하나만 성공")
    void concurrentExchange_onlyOneSucceeds() throws Exception {
        member = memberRepository.save(Member.builder().email("app-race@test.com").nickname("경쟁").password("password123").build());
        String code = appOAuth2Service.issueLoginCode(member.getId(), false,
                AppOAuth2LoginTest.challenge(AppOAuth2LoginTest.VERIFIER));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                ready.await();
                try {
                    appOAuth2Service.exchangeLoginCode(code, AppOAuth2LoginTest.VERIFIER);
                    return true;
                } catch (GlobalException e) {
                    return false;
                }
            }));
        }
        ready.countDown();

        int success = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) success++;
        }
        executor.shutdown();

        assertThat(success).isEqualTo(1);
    }
}
