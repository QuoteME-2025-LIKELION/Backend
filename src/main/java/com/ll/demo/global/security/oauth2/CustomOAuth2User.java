package com.ll.demo.global.security.oauth2;

import com.ll.demo.domain.member.member.entity.Member;
import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final Member member;
    private final Map<String, Object> attributes;
    private final boolean newUser;

    public CustomOAuth2User(Member member, Map<String, Object> attributes, boolean newUser) {
        this.member = member;
        this.attributes = attributes;
        this.newUser = newUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return member.getAuthorities();
    }

    @Override
    public String getName() {
        return member.getEmail();
    }
}
