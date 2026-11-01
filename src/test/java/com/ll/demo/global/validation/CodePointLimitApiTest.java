package com.ll.demo.global.validation;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.security.AuthTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CodePointLimitApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AuthTokenService authTokenService;

    private ResultActions postJson(String url, String body) throws Exception {
        Member member = memberRepository.save(Member.builder().email("limit@test.com").nickname("제한").password("password123").build());
        return mvc.perform(post(url)
                .header("Authorization", "Bearer " + authTokenService.genToken(member, 600))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String quoteBody(String content) {
        return "{\"content\":\"" + content + "\",\"originalContent\":\"원문\"}";
    }

    @Test
    @DisplayName("명언은 코드포인트 30자까지 허용 (UTF-16 54자)")
    void quote_30CodePoints_created() throws Exception {
        postJson("/api/quotes", quoteBody("오늘은좋은날" + "😀".repeat(24))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("명언 코드포인트 31자 → 400")
    void quote_31CodePoints_400() throws Exception {
        postJson("/api/quotes", quoteBody("오늘은좋은날" + "😀".repeat(25))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("허용 문자 그룹명 → 생성")
    void groupName_allowed() throws Exception {
        postJson("/api/groups", "{\"name\":\"우리 모임😀\"}").andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @ValueSource(strings = {"우리!모임", "#", "*"})
    @DisplayName("허용되지 않은 그룹명 → 400")
    void groupName_rejected_400(String name) throws Exception {
        postJson("/api/groups", "{\"name\":\"" + name + "\"}").andExpect(status().isBadRequest());
    }
}
