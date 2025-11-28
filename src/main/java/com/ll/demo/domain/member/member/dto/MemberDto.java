package com.ll.demo.domain.member.member.dto;

import com.ll.demo.domain.member.member.entity.Member;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class MemberDto {
    private Long id;
    private String email;
    private String nickname;
    private String birthYear;
    private LocalDateTime createDate;

    public static MemberDto of(Member member) {
        if (member == null) return null;
        MemberDto dto = new MemberDto();
        dto.id = member.getId();
        dto.email = member.getEmail();
        dto.nickname = member.getNickname();
        dto.birthYear = member.getBirthYear();
        dto.createDate = member.getCreateDate();
        return dto;
    }
}