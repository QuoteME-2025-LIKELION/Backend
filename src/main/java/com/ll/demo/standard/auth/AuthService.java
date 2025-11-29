package com.ll.demo.standard.auth;

import com.ll.demo.domain.member.member.entity.Member;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    // 권한 체크
    public boolean checkCanUseFunction(Member actor) {
        //
        return true;
    }
}