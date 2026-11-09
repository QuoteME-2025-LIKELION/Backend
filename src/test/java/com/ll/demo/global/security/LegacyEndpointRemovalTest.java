package com.ll.demo.global.security;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LegacyEndpointRemovalTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AuthTokenService authTokenService;

    @Test
    @DisplayName("쿼리스트링 가입 GET /join 은 제거되어 회원이 생성되지 않는다")
    void getJoin_removed() throws Exception {
        Member member = memberRepository.save(Member.builder().email("me@test.com").nickname("나").password("password123").build());

        mvc.perform(get("/join").param("email", "legacy@test.com").param("password", "password123").param("birth_year", "2000")
                        .header("Authorization", "Bearer " + authTokenService.genToken(member, 600)))
                .andExpect(status().isNotFound());

        assertThat(memberRepository.findByEmail("legacy@test.com")).isEmpty();
    }

    @Test
    @DisplayName("폼 로그인은 비활성화되어 POST /login 이 처리되지 않는다")
    void formLogin_disabled() throws Exception {
        mvc.perform(post("/login").param("username", "me@test.com").param("password", "password123"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }
}
