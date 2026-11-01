package com.ll.demo.standard.rq;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RqGetMemberTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private AuthTokenService authTokenService;

    private Rq rqWithCookie(String accessToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (accessToken != null) {
            request.setCookies(new Cookie("accessToken", accessToken));
        }
        return new Rq(request, new MockHttpServletResponse(), memberRepository, authTokenService);
    }

    private Member save(String name) {
        return memberRepository.save(Member.builder().email(name + "@test.com").nickname(name).password("password123").build());
    }

    @Test
    @DisplayName("유효한 토큰이면 토큰 주인을 반환 (회원 1 고정 아님)")
    void validToken_returnsOwner() {
        save("first");
        Member owner = save("owner");

        assertThat(rqWithCookie(authTokenService.genToken(owner, 600)).getMember().getId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("쿠키가 없거나 위조 토큰이면 null")
    void missingOrForgedToken_null() {
        save("first");

        assertThat(rqWithCookie(null).getMember()).isNull();
        assertThat(rqWithCookie("forged.token.value").getMember()).isNull();
    }
}
