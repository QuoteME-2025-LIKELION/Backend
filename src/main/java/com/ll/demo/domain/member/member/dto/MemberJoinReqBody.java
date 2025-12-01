package com.ll.demo.domain.member.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberJoinReqBody {

    @Email(message = "올바른 이메일 주소를 입력해주세요.")
    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;

    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다.")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotBlank(message = "출생년도를 입력해주세요.")
    @Size(min = 4, max = 4, message = "출생년도는 4자리로 입력해주세요.")
    private String birthYear;
}
