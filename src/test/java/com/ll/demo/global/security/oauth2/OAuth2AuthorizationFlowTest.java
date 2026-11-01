package com.ll.demo.global.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2AuthorizationFlowTest {

    private static final String SESSION_REQUEST_KEY =
            HttpSessionOAuth2AuthorizationRequestRepository.class.getName() + ".AUTHORIZATION_REQUEST";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("google mode=login: 302로 구글 인가 URL, state 포함, 세션에 모드와 인가 요청 저장, 인가 요청 쿠키 없음")
    void googleLogin_redirectsAndStoresInSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MvcResult result = mvc.perform(get("/oauth2/authorization/google").param("mode", "login").session(session))
                .andExpect(status().isFound())
                .andReturn();

        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).contains("accounts.google.com").contains("state=");
        assertThat(session.getAttribute(OAuth2LoginModeFilter.SESSION_ATTRIBUTE)).isEqualTo("login");
        assertThat(session.getAttribute(SESSION_REQUEST_KEY)).isNotNull();
        assertThat(result.getResponse().getCookie("oauth2_auth_request")).isNull();
    }

    @Test
    @DisplayName("kakao mode=signup: 302로 카카오 인가 URL")
    void kakaoSignup_redirects() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MvcResult result = mvc.perform(get("/oauth2/authorization/kakao").param("mode", "signup").session(session))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(result.getResponse().getRedirectedUrl()).contains("kauth.kakao.com");
        assertThat(session.getAttribute(OAuth2LoginModeFilter.SESSION_ATTRIBUTE)).isEqualTo("signup");
    }

    @Test
    @DisplayName("mode 없이 요청해도 302")
    void noMode_redirects() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound());
    }

    @Test
    @DisplayName("위조 state 콜백은 500이 아니라 실패 URI로 리다이렉트")
    void forgedState_redirectsToFailure() throws Exception {
        MvcResult result = mvc.perform(get("/login/oauth2/code/google").param("code", "x").param("state", "forged"))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(result.getResponse().getRedirectedUrl()).contains("error=oauth2");
    }
}
