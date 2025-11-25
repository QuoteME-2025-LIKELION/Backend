package com.ll.demo.domain.member.member.controller;

import com.ll.demo.domain.member.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberJoinRespBody {

    private Member item;  // 회원가입 후 생성된 회원 정보 받
}