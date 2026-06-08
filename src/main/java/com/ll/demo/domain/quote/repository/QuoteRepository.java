package com.ll.demo.domain.quote.repository;

import java.time.LocalDate;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.Quote;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    // 특정 날짜의 친구들 명언 조호;
    @Query("SELECT q FROM Quote q WHERE q.author.id IN :friendIds AND q.group.id = :groupId AND q.createDate >= :#{#date.atStartOfDay()} AND q.createDate <= :#{#date.atTime(java.time.LocalTime.MAX)} ORDER BY q.createDate DESC")
    List<Quote> findFeedQuotes(@Param("friendIds") List<Long> friendIds, @Param("date") java.time.LocalDate date, @Param("groupId") Long groupId);

    // 나의 명언 조회
    List<Quote> findAllByAuthorIdOrderByCreateDateDesc(Long authorId);

    // 특정 날짜의 전체 명언 조회
    List<Quote> findAllByCreateDateBetweenOrderByCreateDateDesc(LocalDateTime start, LocalDateTime end);

    // 특정 날짜의 특정 작성자 명언 조회 (아카이브 캘린더용)
    List<Quote> findAllByAuthorIdAndCreateDateBetweenOrderByCreateDateDesc(Long authorId, LocalDateTime start, LocalDateTime end);

    // 1일 1명언 체크용
    boolean existsByAuthorIdAndCreateDateBetween(Long authorId, LocalDateTime start, LocalDateTime end);

    // 명언 개수
    long countByAuthor(Member author);
    @Query("select ql.quote from QuoteLike ql where ql.member.id = :memberId order by ql.id desc")
    List<Quote> findQuotesLikedByMember(@Param("memberId") Long memberId);

    // 그룹원 리스트 > 명언 총 개수
    long countByAuthorIn(Collection<Member> authors);

    // 로그인한 사용자가 작성한 거 제외하고 모든 명언 조회
    @Query("SELECT q FROM Quote q WHERE q.author.id <> :userId OR :userId IS NULL")
    List<Quote> findAllExcludingUser(@Param("userId") Long userId, Sort sort);

    // 날짜 범위 필터링
    @Query("SELECT q FROM Quote q WHERE q.createDate >= :startDate AND q.createDate < :endDate ORDER BY q.createDate DESC")
    List<Quote> findAllByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 내가 작성한 글 모두 조회
    List<Quote> findAllByAuthorId(Long authorId);

    // 특정 그룹 멤버들의 날짜 범위 명언 조회
    @Query("SELECT q FROM Quote q WHERE q.author IN :members AND q.createDate >= :startDate AND q.createDate < :endDate ORDER BY q.createDate DESC")
    List<Quote> findAllByAuthorsInAndDateRange(
            @Param("members") Collection<Member> members,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


}
