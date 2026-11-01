package com.ll.demo.domain.quote.entity;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Quote extends BaseTime {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Member author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuoteTag> quoteTags = new ArrayList<>();

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuoteLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuoteTagRequest> tagRequests = new ArrayList<>();

    @Column(nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String originalContent;

    @Column(columnDefinition = "TEXT")
    private String summary;

    public void setCreateDateForDemo(LocalDateTime dateTime) {
        this.createDate = dateTime;
    }
}