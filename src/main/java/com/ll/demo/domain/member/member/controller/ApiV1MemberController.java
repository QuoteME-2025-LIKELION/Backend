package com.ll.demo.domain.member.member.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ll.demo.global.exceptions.GlobalException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ll.demo.domain.member.member.dto.MemberDto;
import com.ll.demo.standard.rq.Rq;
import jakarta.servlet.http.HttpServletResponse;
import com.ll.demo.domain.member.member.dto.MemberLoginReqBody;
import com.ll.demo.domain.member.member.dto.MemberJoinRespBody;
import jakarta.servlet.http.Cookie;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.AppConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParserBuilder;

//REST API용
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

        // birthYear 타입을 String으로 통일...?
        RsData<Member> joinRs = memberService.join(
                reqBody.getEmail(),
                reqBody.getPassword(),
                birthYear
        );

        Member memberEntity = joinRs.getData();
        MemberDto memberDto = MemberDto.of(memberEntity);
        return joinRs.newDataOf(new MemberJoinRespBody(memberDto));
    }

    @PostMapping("/login")
    public RsData<MemberDto> login(
            HttpServletResponse response,
            @RequestBody @Valid MemberLoginReqBody reqBody
    ) {
        Member member = memberService.findByEmail(reqBody.getEmail())
                .orElseThrow(() -> new GlobalException("400-1", "존재하지 않는 회원입니다."));

        if (!memberService.checkPassword(member, reqBody.getPassword())) {
            throw new GlobalException("400-2", "비밀번호가 일치하지 않습니다.");
        }

        String accessToken = authTokenService.genToken(member, AppConfig.getAccessTokenExpirationSec()); // 5분
        String refreshToken = member.getRefreshToken();

        rq.setCookie(response, "accessToken", accessToken, AppConfig.getAccessTokenExpirationSec());
        rq.setCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 30); // 수명 30일

        return RsData.of("로그인 성공", MemberDto.of(member));
    }

    @PostMapping("/logout")
    public RsData<?> logout(HttpServletResponse response) {
        Cookie usernameCookie = new Cookie("actorUsername", "");
        usernameCookie.setMaxAge(0); // 수명을 0으로 설정해서 로그아웃
        usernameCookie.setPath("/");
        response.addCookie(usernameCookie);

        Cookie passwordCookie = new Cookie("actorPassword", "");
        passwordCookie.setMaxAge(0);
        passwordCookie.setPath("/");
        response.addCookie(passwordCookie);

        return RsData.of("200-2", "로그아웃 성공");
    }

}