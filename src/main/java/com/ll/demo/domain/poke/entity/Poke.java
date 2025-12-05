package com.ll.demo.domain.poke.entity;

import static lombok.AccessLevel.PROTECTED;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Poke extends BaseTime {

    // 찌른 사람 (보낸 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    private Member sender;

    // 찔린 사람 (받는 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    private Member receiver;
}