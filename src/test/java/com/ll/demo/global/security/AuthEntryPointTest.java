package com.ll.demo.global.security;

import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthEntryPointTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberService memberService;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private GroupRepository groupRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .email("entry@test.com").nickname("입구").password("password123").build());
    }

    @Test
    @DisplayName("토큰 없음 → 401, 401-4, 로그인이 필요합니다.")
    void noToken_401_4() throws Exception {
        mvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-4"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("만료된 access 토큰 → 401, 401-5, 인증이 만료되었습니다.")
    void expiredToken_401_5() throws Exception {
        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + authTokenService.genToken(member, -60)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-5"))
                .andExpect(jsonPath("$.message").value("인증이 만료되었습니다."));
    }

    @Test
    @DisplayName("refresh 토큰을 access로 보냄 → 401-5")
    void refreshAsAccess_401_5() throws Exception {
        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + memberService.genRefreshToken(member)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-5"));
    }

    @Test
    @DisplayName("형식이 깨진 토큰 → 401-5")
    void malformedToken_401_5() throws Exception {
        mvc.perform(get("/api/profile").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-5"));
    }

    @Test
    @DisplayName("만료 access 쿠키 + 무효 refresh 쿠키 → 401-5")
    void expiredCookieWithInvalidRefresh_401_5() throws Exception {
        mvc.perform(get("/api/profile").cookie(
                        new Cookie("accessToken", authTokenService.genToken(member, -60)),
                        new Cookie("refreshToken", "stale")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-5"));
    }

    @Test
    @DisplayName("남의 리소스(가입하지 않은 그룹 피드) → 403 유지")
    void othersResource_403() throws Exception {
        Member leader = memberRepository.save(Member.builder()
                .email("leader@test.com").nickname("리더").password("password123").build());
        Group group = groupRepository.save(Group.builder().name("남의그룹").motto("m").leader(leader).build());

        mvc.perform(get("/api/quotes/feed?date=" + LocalDate.now() + "&groupId=" + group.getId())
                        .header("Authorization", "Bearer " + authTokenService.genToken(member, 600)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("웹 쿠키 자동 재발급(만료 access + 유효 refresh 쿠키)은 여전히 200")
    void webCookieReissue_200() throws Exception {
        mvc.perform(get("/api/profile").cookie(
                        new Cookie("accessToken", authTokenService.genToken(member, -60)),
                        new Cookie("refreshToken", memberService.genRefreshToken(member))))
                .andExpect(status().isOk());
    }
}
