package com.ll.demo.domain.member.member.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//REST API용
@RestController
@RequestMapping("/api/auth/signup")
@RequiredArgsConstructor
@Slf4j
public class ApiV1MemberController {

    private final MemberService memberService;

    @PostMapping("")
    public RsData<MemberJoinRespBody> join(@RequestBody @Valid MemberJoinReqBody reqBody) {
        Integer birthYear = Integer.parseInt(reqBody.getBirthYear());

        // birthYear 타입을 String으로 통일...?
        RsData<Member> joinRs = memberService.join(
                reqBody.getEmail(),
                reqBody.getPassword(),
                birthYear
        );

        return joinRs.newDataOf(new MemberJoinRespBody(joinRs.getData()));
    }
}