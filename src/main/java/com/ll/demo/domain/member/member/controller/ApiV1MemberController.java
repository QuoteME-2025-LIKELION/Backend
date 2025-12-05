package com.ll.demo.domain.member.member.controller;

import com.ll.demo.AppConfig;
import com.ll.demo.domain.member.member.dto.MemberDto;
import com.ll.demo.domain.member.member.dto.MemberJoinReqBody;
import com.ll.demo.domain.member.member.dto.MemberJoinRespBody;
import com.ll.demo.domain.member.member.dto.MemberLoginReqBody;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.standard.rq.Rq;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class ApiV1MemberController {

    private final MemberService memberService;
    private final Rq rq;
    private final AuthTokenService authTokenService;

    @PostMapping("/signup")
    public RsData<MemberJoinRespBody> join(@RequestBody @Valid MemberJoinReqBody reqBody) {
        Integer birthYear = Integer.parseInt(reqBody.getBirthYear());

        RsData<Member> joinRs = memberService.join(
                reqBody.getEmail(),
                reqBody.getPassword(),
                birthYear
        );

        Member memberEntity = joinRs.getData();
        MemberDto memberDto = MemberDto.of(memberEntity);
        return joinRs.newDataOf(new MemberJoinRespBody(memberDto));
    }

    @Getter
    @AllArgsConstructor
    public static class LoginResponseBody {
        private MemberDto item;
        private String accessToken;
    }

    @PostMapping("/login")
    public RsData<LoginResponseBody> login(
            HttpServletResponse response,
            @RequestBody @Valid MemberLoginReqBody reqBody
    ) {
        Member member = memberService.findByEmail(reqBody.getEmail())
                .orElseThrow(() -> new GlobalException("400-1", "존재하지 않는 회원입니다."));

        if (!memberService.checkPassword(member, reqBody.getPassword())) {
            throw new GlobalException("400-2", "비밀번호가 일치하지 않습니다.");
        }

        // 토큰 생성
        String accessToken = authTokenService.genToken(member, AppConfig.getAccessTokenExpirationSec()); // 5분
        String refreshToken = member.getRefreshToken();

        // 쿠키에도 담고 (기존 로직 유지)
        rq.setCookie(response, "accessToken", accessToken, AppConfig.getAccessTokenExpirationSec());
        rq.setCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 30); // 수명 30일

        // ▼▼▼ [수정됨] Body에도 담아서 리턴! ▼▼▼
        return RsData.of(
                "로그인 성공",
                new LoginResponseBody(MemberDto.of(member), accessToken)
        );
    }

    @PostMapping("/logout")
    public RsData<?> logout(HttpServletResponse response) {
        Cookie usernameCookie = new Cookie("actorUsername", "");
        usernameCookie.setMaxAge(0);
        usernameCookie.setPath("/");
        response.addCookie(usernameCookie);

        Cookie passwordCookie = new Cookie("actorPassword", "");
        passwordCookie.setMaxAge(0);
        passwordCookie.setPath("/");
        response.addCookie(passwordCookie);

        return RsData.of("200-2", "로그아웃 성공");
    }
}