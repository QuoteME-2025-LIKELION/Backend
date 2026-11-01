package com.ll.demo.domain.quote.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import com.ll.demo.domain.quote.entity.TagRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteTagRequestRepository extends JpaRepository<QuoteTagRequest, Long> {
    // 특정 글에 특정 멤버가 이미 태그 요청을 했는지 확인
    @Query("SELECT r.quote.id FROM QuoteTagRequest r WHERE r.requester.id = :requesterId AND r.quote.id IN :quoteIds AND r.status = :status")
    Set<Long> findQuoteIdsByRequesterAndStatus(
            @Param("requesterId") Long requesterId,
            @Param("quoteIds") List<Long> quoteIds,
            @Param("status") TagRequestStatus status
    );

    List<QuoteTagRequest> findAllByQuoteIdAndStatus(Long quoteId, TagRequestStatus status);

    Optional<QuoteTagRequest> findByQuoteAndRequester(Quote quote, Member requester);

    @Modifying
    @Query("DELETE FROM QuoteTagRequest r WHERE r.requester = :member OR r.quote IN (SELECT q FROM Quote q WHERE q.author = :member)")
    void deleteAllRelatedToMember(@Param("member") Member member);

}
