package com.ll.demo.domain.friendship.friendship.controller;

import org.springframework.web.server.ResponseStatusException;
import com.ll.demo.domain.friendship.friendship.service.FriendshipService;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.SecurityUser;
import com.ll.demo.domain.member.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;
    @PostMapping("/add/{targetId}")
    public ResponseEntity<RsData> addFriendship(
            @PathVariable Long targetId,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        if (securityUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "401-1. 로그인 인증 정보가 유효하지 않습니다."
            );
        }
        Member memberA = securityUser.getMember();
        friendshipService.addFriendship(memberA, targetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(RsData.of("201-1", "친구로 등록되었습니다."));
    }
}