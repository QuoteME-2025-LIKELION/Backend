package com.ll.demo.domain.profile.profile.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.member.member.type.MemberProvider;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileSetupTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberService memberService;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private EntityManager em;

    private Member socialMember;

    @BeforeEach
    void setUp() {
        socialMember = memberService.findOrCreateSocialMember(
                MemberProvider.KAKAO, "setup-1", "setup@test.com", true, "카카오닉", null, true).member();
    }

    private String bearer(Member member) {
        return "Bearer " + authTokenService.genToken(member, 600);
    }

    private ResultActions setup(Member as, String body) throws Exception {
        return mvc.perform(put("/api/profile/setup")
                .header("Authorization", bearer(as))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private boolean completed(Member member) {
        em.flush();
        em.clear();
        return memberRepository.findById(member.getId()).orElseThrow().isProfileCompleted();
    }

    @Test
    @DisplayName("소셜 신규 회원은 false로 시작하고 setup 저장 후 true")
    void socialNewMember_setupCompletes() throws Exception {
        assertThat(completed(socialMember)).isFalse();
        mvc.perform(get("/api/profile").header("Authorization", bearer(socialMember)))
                .andExpect(jsonPath("$.profileCompleted").value(false));

        setup(socialMember, "{\"nickname\":\"새닉\",\"birthYear\":\"2001\",\"introduction\":\"안녕\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새닉"))
                .andExpect(jsonPath("$.introduction").value("안녕"))
                .andExpect(jsonPath("$.profileCompleted").value(true));

        assertThat(completed(socialMember)).isTrue();
        Member saved = memberRepository.findById(socialMember.getId()).orElseThrow();
        assertThat(saved.getBirthYear()).isEqualTo("2001");
    }

    @Test
    @DisplayName("소개는 선택이고, 이미 완료한 회원이 다시 호출해도 값이 갱신된다")
    void introductionOptional_andRepeatable() throws Exception {
        setup(socialMember, "{\"nickname\":\"첫닉\",\"birthYear\":\"2001\"}").andExpect(status().isOk());
        setup(socialMember, "{\"nickname\":\"둘째닉\",\"birthYear\":\"1999\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("둘째닉"))
                .andExpect(jsonPath("$.profileCompleted").value(true));

        assertThat(memberRepository.findById(socialMember.getId()).orElseThrow().getBirthYear()).isEqualTo("1999");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"birthYear\":\"2001\"}",
            "{\"nickname\":\" \",\"birthYear\":\"2001\"}",
            "{\"nickname\":\"닉\"}",
            "{\"nickname\":\"닉\",\"birthYear\":\"20a1\"}",
            "{\"nickname\":\"닉\",\"birthYear\":\"1800\"}",
            "{\"nickname\":\"열한글자닉네임입니다요\",\"birthYear\":\"2001\"}",
            "{\"nickname\":\"닉\",\"birthYear\":\"2001\",\"introduction\":\"스물한글자자기소개입니다스물한글자자기소개\"}"
    })
    @DisplayName("필수 항목 누락·형식 오류 → 400, 완료 처리하지 않음")
    void invalid_400(String body) throws Exception {
        setup(socialMember, body).andExpect(status().isBadRequest());

        assertThat(completed(socialMember)).isFalse();
    }

    @Test
    @DisplayName("게스트 setup → 403")
    void guest_403() throws Exception {
        Member guest = memberService.findOrCreateGuest();

        setup(guest, "{\"nickname\":\"닉\",\"birthYear\":\"2001\"}").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게스트는 완료 상태(true)")
    void guest_isCompleted() {
        assertThat(completed(memberService.findOrCreateGuest())).isTrue();
    }

    @Test
    @DisplayName("기존 PUT /api/profile, PUT /api/profile/account 로는 완료 상태가 바뀌지 않는다")
    void legacyApis_doNotComplete() throws Exception {
        MockMultipartFile data = new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"nickname\":\"직접닉\",\"introduction\":\"소개\"}".getBytes());
        mvc.perform(multipart("/api/profile").file(data)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", bearer(socialMember)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileCompleted").value(false));
        mvc.perform(put("/api/profile/account")
                        .header("Authorization", bearer(socialMember))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"setup@test.com\",\"birthYear\":\"2000\",\"gender\":\"MALE\"}"))
                .andExpect(status().isOk());

        assertThat(completed(socialMember)).isFalse();
    }

    @Test
    @DisplayName("이메일 가입 회원은 false로 시작하고 가입·로그인 응답 MemberDto에 profileCompleted가 나온다")
    void emailSignup_startsIncomplete() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"email-new@test.com\",\"password\":\"password123\",\"birthYear\":\"2000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.profileCompleted").value(false));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"email-new@test.com\",\"password\":\"password123\"}"))
                .andExpect(jsonPath("$.data.item.profileCompleted").value(false));
    }

    @Test
    @DisplayName("빌더로 만든 기존 경로 회원은 true (DDL 기본값과 같음)")
    void builderDefault_isCompleted() {
        Member member = memberRepository.save(Member.builder().email("old@test.com").nickname("옛").password("password123").build());

        assertThat(completed(member)).isTrue();
    }

    @Test
    @DisplayName("기존 행: profile_completed 없이 넣은 행은 DDL 기본값으로 true")
    void existingRow_defaultsTrue() {
        em.createNativeQuery("insert into members (email, ai_usage_count) values ('legacy-row@test.com', 0)").executeUpdate();

        Object value = em.createNativeQuery("select profile_completed from members where email = 'legacy-row@test.com'")
                .getSingleResult();
        assertThat(value).isEqualTo(true);
    }
}
