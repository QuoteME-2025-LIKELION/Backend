package com.ll.demo.global.security;

import com.ll.demo.domain.member.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class SecurityUser extends User {
    private final Member member;

    public SecurityUser(Member member, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        // 부모 클래스(User)의 생성자 호출 (아이디, 비번, 권한)
        super(username, password, authorities);
        this.member = member;
    }
}