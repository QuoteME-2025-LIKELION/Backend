package com.ll.demo.domain.member.member.dto;

import com.ll.demo.domain.member.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.time.LocalDateTime;
import lombok.Builder;

@Getter
public class MemberDto {
    private Long id;
    private String email;
    private String nickname;
    private String birthYear;
    private LocalDateTime createDate;
    @Schema(description = "PUT /api/profile/setup으로 닉네임·출생년도를 저장하면 true. 신규 가입은 false, 기존 회원은 true")
    private boolean profileCompleted;

    public static MemberDto of(Member member) {
        if (member == null) return null;
        MemberDto dto = new MemberDto();
        dto.id = member.getId();
        dto.email = member.getEmail();
        dto.nickname = member.getNickname();
        dto.birthYear = member.getBirthYear();
        dto.createDate = member.getCreateDate();
        dto.profileCompleted = member.isProfileCompleted();
        return dto;
    }
}