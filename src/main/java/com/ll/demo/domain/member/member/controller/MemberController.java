package com.ll.demo.domain.member.member.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.standard.dto.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/join")
    @ResponseBody
    public RsData join(
            String email, String password, Int birth_year
    ) {
        if (Ut.str.isBlank(email)) {
            throw new GlobalException("400-1", "이메일 입력");
        }
        if (Ut.str.isBlank(password)) {
            throw new GlobalException("400-2", "비밀번호 입력");
        }
        if (Ut.str.isBlank(birth_year)) {
            throw new GlobalException("400-3", "출생년도(yyyy) 입력");
        }
        RsData<Member> joinRs = memberService.join(email, password, birth_year);

        return joinRs;
    }
}