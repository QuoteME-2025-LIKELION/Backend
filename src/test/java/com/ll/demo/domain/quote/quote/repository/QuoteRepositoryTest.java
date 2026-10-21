package com.ll.demo.domain.quote.quote.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuoteRepositoryTest {

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private MemberRepository memberRepository;

    private final LocalDate date = LocalDate.of(2026, 10, 20);
    private final LocalDateTime start = date.atStartOfDay();
    private final LocalDateTime end = date.plusDays(1).atStartOfDay();

    private Member friend;
    private Member otherFriend;
    private Member stranger;

    @BeforeEach
    void setUp() {
        friend = saveMember("feed-friend@test.com", "친구");
        otherFriend = saveMember("feed-other@test.com", "다른친구");
        stranger = saveMember("feed-stranger@test.com", "타인");
    }

    @Test
    @DisplayName("지정한 작성자의 해당 날짜 명언만 최신순으로 조회한다")
    void findFeedQuotes_returnsOnlyGivenAuthorsOfTheDate_latestFirst() {
        saveQuote(friend, "아침", date.atTime(9, 0));
        saveQuote(otherFriend, "저녁", date.atTime(18, 0));
        saveQuote(stranger, "친구 아님", date.atTime(12, 0));

        List<Quote> result = quoteRepository.findFeedQuotes(
                List.of(friend.getId(), otherFriend.getId()), start, end);

        assertThat(result).extracting(Quote::getContent).containsExactly("저녁", "아침");
    }

    @Test
    @DisplayName("날짜 경계: 당일 00:00:00~23:59:59는 포함하고 전날 말/다음 날 0시는 제외한다")
    void findFeedQuotes_dateBoundaries() {
        saveQuote(friend, "전날 마지막", date.minusDays(1).atTime(23, 59, 59));
        saveQuote(friend, "당일 처음", start);
        saveQuote(friend, "당일 마지막", date.atTime(23, 59, 59));
        saveQuote(friend, "다음날 처음", end);

        List<Quote> result = quoteRepository.findFeedQuotes(List.of(friend.getId()), start, end);

        assertThat(result).extracting(Quote::getContent).containsExactly("당일 마지막", "당일 처음");
    }

    @Test
    @DisplayName("해당 날짜에 명언이 없으면 예외 없이 빈 목록을 반환한다")
    void findFeedQuotes_noQuotes_returnsEmpty() {
        List<Quote> result = quoteRepository.findFeedQuotes(List.of(friend.getId()), start, end);

        assertThat(result).isEmpty();
    }

    private Member saveMember(String email, String nickname) {
        return memberRepository.save(Member.builder()
                .email(email).nickname(nickname).password("password123").build());
    }

    private Quote saveQuote(Member author, String content, LocalDateTime createDate) {
        Quote quote = quoteRepository.save(Quote.builder().author(author).content(content).build());
        quote.setCreateDateForDemo(createDate);
        return quoteRepository.saveAndFlush(quote);
    }

        @Test
    @DisplayName("월 조회: 말일 23:59:59와 1일 00:00:00은 포함, 전월 말/다음 달 1일 0시는 제외하고 최신순")
    void findMonthQuotes_boundaries_latestFirst() {
        Member me = saveMember("month-me@test.com", "나");
        Member other = saveMember("month-other@test.com", "다른사람");
        YearMonth month = YearMonth.of(2026, 9);

        saveQuote(me, "전월 말", LocalDateTime.of(2026, 8, 31, 23, 59, 59));
        saveQuote(me, "1일 처음", LocalDateTime.of(2026, 9, 1, 0, 0, 0));
        saveQuote(me, "중간", LocalDateTime.of(2026, 9, 15, 12, 0, 0));
        saveQuote(me, "말일 마지막", LocalDateTime.of(2026, 9, 30, 23, 59, 59));
        saveQuote(me, "다음 달 처음", LocalDateTime.of(2026, 10, 1, 0, 0, 0));
        saveQuote(other, "타인", LocalDateTime.of(2026, 9, 10, 10, 0, 0));

        List<Quote> result = quoteRepository
                .findAllByAuthorIdAndCreateDateGreaterThanEqualAndCreateDateLessThanOrderByCreateDateDesc(
                        me.getId(), month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());

        assertThat(result).extracting(Quote::getContent)
                .containsExactly("말일 마지막", "중간", "1일 처음");
    }

    @Test
    @DisplayName("월 조회: 해당 월에 명언이 없으면 빈 목록을 반환한다")
    void findMonthQuotes_emptyMonth_returnsEmpty() {
        Member me = saveMember("month-empty@test.com", "나");
        saveQuote(me, "8월", LocalDateTime.of(2026, 8, 10, 10, 0, 0));

        List<Quote> result = quoteRepository
                .findAllByAuthorIdAndCreateDateGreaterThanEqualAndCreateDateLessThanOrderByCreateDateDesc(
                        me.getId(), LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("전체 조회: 서로 다른 날짜 3건을 임의 순서로 저장해도 항상 최신순")
    void findAllByAuthorIdOrderByCreateDateDesc_isDeterministic() {
        Member me = saveMember("order-me@test.com", "나");
        saveQuote(me, "가운데", LocalDateTime.of(2026, 9, 10, 10, 0));
        saveQuote(me, "최신", LocalDateTime.of(2026, 9, 20, 10, 0));
        saveQuote(me, "오래됨", LocalDateTime.of(2026, 9, 1, 10, 0));

        List<Quote> result = quoteRepository.findAllByAuthorIdOrderByCreateDateDesc(me.getId());

        assertThat(result).extracting(Quote::getContent).containsExactly("최신", "가운데", "오래됨");
    }
}