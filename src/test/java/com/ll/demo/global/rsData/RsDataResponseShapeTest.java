package com.ll.demo.global.rsData;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.global.security.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RsDataResponseShapeTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private AuthTokenService authTokenService;

    private Member author;
    private Member requester;
    private Quote quote;

    @BeforeEach
    void setUp() {
        author = save("author");
        requester = save("requester");
        quote = quoteRepository.save(Quote.builder().author(author).content("명언").originalContent("원문").build());
    }

    private Member save(String name) {
        return memberRepository.save(Member.builder().email(name + "@test.com").nickname(name).password("password123").build());
    }

    private MockHttpServletRequestBuilder as(Member member, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + authTokenService.genToken(member, 600));
    }

    @Test
    @DisplayName("태그 요청: resultCode 201-3, 문구는 message, data는 null")
    void tagRequest() throws Exception {
        mvc.perform(as(requester, post("/api/quotes/" + quote.getId() + "/tag-request")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-3"))
                .andExpect(jsonPath("$.message").value("태그 요청이 명언 작성자에게 전송되었습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("북마크: resultCode 201, 문구는 message")
    void bookmark() throws Exception {
        mvc.perform(as(requester, post("/api/quotes/" + quote.getId() + "/bookmark")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("북마크")))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("친구 요청 수락: resultCode 200, 문구는 message")
    void friendAccept() throws Exception {
        Friendship request = friendshipRepository.save(Friendship.builder()
                .member(requester).friend(author).status(FriendshipStatus.REQUESTED).build());

        mvc.perform(as(author, post("/api/friends/requests/" + request.getId() + "/accept")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.message").value("친구 요청을 수락했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("미인증 응답: 상태 401, resultCode 401-4, 문구는 message")
    void unauthenticated() throws Exception {
        mvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-4"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }
}
