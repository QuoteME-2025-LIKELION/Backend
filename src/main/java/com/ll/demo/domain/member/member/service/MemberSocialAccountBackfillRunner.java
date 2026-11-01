package com.ll.demo.domain.member.member.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.entity.MemberSocialAccount;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.repository.MemberSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 기존 members.provider/providerId를 소셜 계정 테이블로 이행 (여러 번 실행해도 결과 동일)
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberSocialAccountBackfillRunner implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final MemberSocialAccountRepository memberSocialAccountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // members.provider는 숫자, 소셜 계정 테이블은 문자열로 저장되어 SQL로 직접 비교하지 않고 메모리에서 대조
        List<MemberSocialAccount> existing = memberSocialAccountRepository.findAll();
        Set<String> linkedMembers = new HashSet<>();
        Set<String> usedProviderIds = new HashSet<>();
        for (MemberSocialAccount account : existing) {
            linkedMembers.add(account.getMember().getId() + ":" + account.getProvider());
            usedProviderIds.add(account.getProvider() + ":" + account.getProviderId());
        }

        List<Member> members = memberRepository.findAllWithSocialProvider().stream()
                .filter(member -> !linkedMembers.contains(member.getId() + ":" + member.getProvider()))
                .toList();

        // 같은 소셜 ID를 가진 회원이 여럿이면 유니크 제약을 지키기 위해 먼저 나온 회원만 이행
        List<MemberSocialAccount> accounts = members.stream()
                .filter(member -> usedProviderIds.add(member.getProvider() + ":" + member.getProviderId()))
                .map(member -> MemberSocialAccount.builder()
                        .member(member)
                        .provider(member.getProvider())
                        .providerId(member.getProviderId())
                        .build())
                .toList();

        memberSocialAccountRepository.saveAll(accounts);
        log.info("소셜 계정 이행 완료: 생성 {}건, 건너뜀 {}건", accounts.size(), members.size() - accounts.size());
    }
}
