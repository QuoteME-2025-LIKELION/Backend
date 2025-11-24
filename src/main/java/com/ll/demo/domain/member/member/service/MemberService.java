package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 이메일로 회원 조회
    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    // 회원가입
    @Transactional
    public RsData<Member> join(String email, String password, String nickname, Int birthYear) {
        // 이메일 중복 체크
        findByEmail(email).ifPresent(ignored -> {
            throw new GlobalException("400-1", "이미 존재하는 이메일");
        });

        // 새로운 회원 객체
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))  // 비밀번호 암호화
                .nickname(nickname)
                .birthYear(birthYear)
                .build();
        memberRepository.save(member);
        return RsData.of("회원가입 완료", member);
    }

    // ID로 회원 조회?
    public Member getMemberById(long id) {
        return memberRepository.findById(id).orElseThrow(() -> new GlobalException("400-2", "회원이 존재하지 않습니다."));
    }
}
