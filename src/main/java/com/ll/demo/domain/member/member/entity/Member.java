package com.ll.demo.domain.member.member.entity;

import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "members")
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Getter
@Setter
public class Member extends BaseTime {

    @Email(message = "올바른 이메일 주소를 입력해주세요.")
    @Column(unique = true, nullable = false)
    private String email;

    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Column(nullable = false)  // 비밀번호는 필수
    private String password;

    @Size(max = 10, message = "닉네임은 10자 이내여야 합니다.")
    @Column(nullable = true)
    private String nickname;

    @Size(min = 4, max = 4, message = "출생년도는 4자리로 입력해주세요.")
    @Column(nullable = false)
    private String birthYear;

    @Column(length = 255)  // 프로필 사진 - 일단 URL
    private String profileImage;

    @Size(max = 30, message = "자기소개는 30자 이내로 작성해주세요.")
    @Column(length = 255)
    private String bio;

    @Column(nullable = true, length = 255)
    private String refreshToken;
}
