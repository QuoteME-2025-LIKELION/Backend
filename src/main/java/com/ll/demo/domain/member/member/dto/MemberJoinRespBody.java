package com.ll.demo.domain.member.member.dto;
import com.ll.demo.domain.member.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberJoinRespBody {

    private MemberDto item;  // 엔티티>DTO로 변경
}