package com.ll.demo.domain.member.member.controller;

import com.ll.demo.domain.member.member.dto.MemberLoginReqBody;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.standard.rq.Rq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiV1MemberControllerLoginTest {

    @Mock private MemberService memberService;
    @Mock private Rq rq;
    @Mock private AuthTokenService authTokenService;
    @Mock private MemberRepository memberRepository;

    private ApiV1MemberController controller;
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        controller = new ApiV1MemberController(memberService, rq, authTokenService, memberRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 이메일이면 404-1이고 토큰을 발급하지 않는다")
    void login_unknownEmail_404() {
        when(memberService.findByEmail("none@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(new MemberLoginReqBody("none@test.com", "pw123456"), response))
                .isInstanceOf(GlobalException.class)
                .hasMessageContaining("404-1");

        verifyNoInteractions(authTokenService);
    }

    @Test
    @DisplayName("실패: 비밀번호가 틀리면 401-1이고 토큰과 쿠키를 발급하지 않는다")
    void login_wrongPassword_401() {
        Member member = Member.builder().email("a@test.com").password("hash").build();
        when(memberService.findByEmail("a@test.com")).thenReturn(Optional.of(member));
        when(memberService.checkPassword(member, "wrong")).thenReturn(false);

        assertThatThrownBy(() -> controller.login(new MemberLoginReqBody("a@test.com", "wrong"), response))
                .isInstanceOf(GlobalException.class)
                .hasMessageContaining("401-1");

        verifyNoInteractions(authTokenService);
        verify(memberService, never()).genRefreshToken(any());
        verify(rq, never()).setCookie(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("성공: 비밀번호가 맞으면 토큰을 발급하고 쿠키를 설정한다")
    void login_correctPassword_issuesTokens() {
        Member member = Member.builder().email("a@test.com").password("hash").nickname("닉").build();
        when(memberService.findByEmail("a@test.com")).thenReturn(Optional.of(member));
        when(memberService.checkPassword(member, "pw123456")).thenReturn(true);
        when(authTokenService.genToken(any(Member.class), anyInt())).thenReturn("access");
        when(memberService.genRefreshToken(member)).thenReturn("refresh");

        var result = controller.login(new MemberLoginReqBody("a@test.com", "pw123456"), response);

        assertThat(result.getData().getAccessToken()).isEqualTo("access");
        verify(rq).setCookie(response, "accessToken", "access", 300);
        verify(rq).setCookie(response, "refreshToken", "refresh", 60 * 60 * 24 * 30);
    }
}