package com.ll.demo.domain.quote.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.Bookmark;
import com.ll.demo.domain.quote.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMemberAndQuote(Member member, Quote quote);

    Optional<Bookmark> findByMemberAndQuote(Member member, Quote quote);

    @Query("SELECT b.quote FROM Bookmark b WHERE b.member.id = :memberId ORDER BY b.createDate DESC")
    List<Quote> findQuotesByMemberId(@Param("memberId") Long memberId);
}
