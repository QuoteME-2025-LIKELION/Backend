package com.ll.demo.domain.quote.entity;

import static lombok.AccessLevel.PROTECTED;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class QuoteTagRequest extends BaseTime {


    @ManyToOne(fetch = FetchType.LAZY)
    private Quote quote; // 요청 대상 글

    @ManyToOne(fetch = FetchType.LAZY)
    private Member requester; // 태그를 요청한 사람 (조른 사람)

    @Enumerated(EnumType.STRING)
    private TagRequestStatus status; // PENDING(대기), ACCEPTED(수락), REJECTED(거절)

    public void accept() {
        this.status = TagRequestStatus.ACCEPTED;
    }

    public void reject() {
        this.status = TagRequestStatus.REJECTED;
    }
}