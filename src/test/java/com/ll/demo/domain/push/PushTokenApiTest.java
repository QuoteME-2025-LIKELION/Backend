package com.ll.demo.domain.push;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.push.entity.DevicePlatform;
import com.ll.demo.domain.push.entity.DeviceToken;
import com.ll.demo.domain.push.repository.DeviceTokenRepository;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PushTokenApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private DeviceTokenRepository deviceTokenRepository;
    @Autowired private EntityManager em;

    private Member a;
    private Member b;

    @BeforeEach
    void setUp() {
        a = memberRepository.save(Member.builder().email("token-a@test.com").nickname("에이").password("password123").build());
        b = memberRepository.save(Member.builder().email("token-b@test.com").nickname("비").password("password123").build());
    }

    private ResultActions register(Member member, String token, String platform) throws Exception {
        return mvc.perform(post("/api/push/tokens")
                .header("Authorization", "Bearer " + authTokenService.genToken(member, 600))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"platform\":\"" + platform + "\"}"));
    }

    private ResultActions unregister(Member member, String token) throws Exception {
        return mvc.perform(delete("/api/push/tokens")
                .header("Authorization", "Bearer " + authTokenService.genToken(member, 600))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"));
    }

    private DeviceToken reload(String token) {
        em.flush();
        em.clear();
        return deviceTokenRepository.findByToken(token).orElseThrow();
    }

    @Test
    @DisplayName("토큰 등록")
    void register_saves() throws Exception {
        register(a, "token-1", "WEB").andExpect(status().isOk());

        DeviceToken saved = reload("token-1");
        assertThat(saved.getMember().getId()).isEqualTo(a.getId());
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.WEB);
        assertThat(saved.getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 토큰을 다른 회원이 등록하면 소유자 이전, 행은 1개")
    void register_sameToken_transfersOwner() throws Exception {
        register(a, "shared-device", "ANDROID").andExpect(status().isOk());
        register(b, "shared-device", "ANDROID").andExpect(status().isOk());

        DeviceToken saved = reload("shared-device");
        assertThat(saved.getMember().getId()).isEqualTo(b.getId());
        assertThat(deviceTokenRepository.countByMember(a)).isZero();
        assertThat(deviceTokenRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("게스트 계정 등록 → 403")
    void guest_403() throws Exception {
        Member guest = memberRepository.findByEmail("guest@guest.com").orElseGet(() ->
                memberRepository.save(Member.builder().email("guest@guest.com").nickname("게스트").password("password123").build()));

        register(guest, "guest-token", "WEB").andExpect(status().isForbidden());

        assertThat(deviceTokenRepository.findByToken("guest-token")).isEmpty();
    }

    @Test
    @DisplayName("잘못된 플랫폼·빈 토큰 → 400")
    void invalidRequest_400() throws Exception {
        register(a, "token-1", "DESKTOP").andExpect(status().isBadRequest());
        register(a, "", "WEB").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("삭제는 본인 토큰만")
    void unregister_ownOnly() throws Exception {
        register(a, "a-token", "IOS").andExpect(status().isOk());

        unregister(b, "a-token").andExpect(status().isOk());
        em.flush();
        em.clear();
        assertThat(deviceTokenRepository.findByToken("a-token")).isPresent();

        unregister(a, "a-token").andExpect(status().isOk());
        em.flush();
        em.clear();
        assertThat(deviceTokenRepository.findByToken("a-token")).isEmpty();
    }
}
