package com.ll.demo.domain.member.member.entity;

import static lombok.AccessLevel.PROTECTED;
import com.ll.demo.domain.member.member.type.Gender;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

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
    @Column(nullable = false)
    private String password;

    @Size(max = 10, message = "닉네임은 10자 이내여야 합니다.")
    @Column(nullable = true)
    private String nickname;

    @Size(min = 4, max = 4, message = "출생년도는 4자리로 입력해주세요.")
    @Column(nullable = false)
    private String birthYear;

    @Column(length = 255)
    private String profileImage;

    @Size(max = 30, message = "자기소개는 30자 이내로 작성해주세요.")
    @Column(nullable = true)
    private String introduction;

    @Column(nullable = true, length = 255)
    private String refreshToken;

    public String getUsername() {
        return this.email;
    }

    // 시큐리티 - 권한
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public String getName() {
        if (this.nickname != null && !this.nickname.isBlank()) {
            return this.nickname;
        }
        return this.email.split("@")[0]; // 예: test@naver.com -> test
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Gender gender;
}