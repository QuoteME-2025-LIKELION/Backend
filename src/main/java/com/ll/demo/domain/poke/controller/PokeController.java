package com.ll.demo.domain.poke.controller;

import com.ll.demo.domain.poke.service.PokeService;
import com.ll.demo.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pokes")
@RequiredArgsConstructor
public class PokeController {

    private final PokeService pokeService;

    // 1. 콕 찌르기
    // POST /api/pokes/{receiverId}
    @PostMapping("/{receiverId}")
    public ResponseEntity<Void> pokeMember(
            @PathVariable Long receiverId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        pokeService.poke(user.getMember().getId(), receiverId);
        return ResponseEntity.ok().build();
    }

    // 2. 콕 찌르기 통계 (내가 받은 횟수)
    // GET /api/pokes/statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getPokeStatistics(
            @AuthenticationPrincipal SecurityUser user
    ) {
        long count = pokeService.countMyPokes(user.getMember().getId());

        return ResponseEntity.ok(Map.of(
                "receivedCount", count
        ));
    }
}