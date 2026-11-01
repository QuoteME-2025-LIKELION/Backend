package com.ll.demo.global.exceptionHandlers;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.entity.Notification;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.global.security.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GlobalExceptionHandlingTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AuthTokenService authTokenService;

    private Member a;
    private Member b;
    private Member c;

    @BeforeEach
    void setUp() {
        a = save("a@test.com", "에이");
        b = save("b@test.com", "비");
        c = save("c@test.com", "씨");
    }

    private Member save(String email, String nickname) {
        return memberRepository.save(Member.builder().email(email).nickname(nickname).password("password123").build());
    }

    private ResultActions perform(MockHttpServletRequestBuilder req, Member as) throws Exception {
        return mvc.perform(req.header("Authorization", "Bearer " + authTokenService.genToken(as, 600)))
                .andExpect(jsonPath("$.data.trace").doesNotExist());
    }

    @Test
    @DisplayName("친구 중복 요청 → 409")
    void duplicateFriendRequest_409() throws Exception {
        perform(post("/api/friends/request/" + b.getId()), a).andExpect(status().isCreated());
        perform(post("/api/friends/request/" + b.getId()), a)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409"));
    }

    @Test
    @DisplayName("남의 친구 요청 수락 → 403")
    void acceptOthersFriendRequest_403() throws Exception {
        Friendship req = friendshipRepository.save(Friendship.builder()
                .member(a).friend(b).status(FriendshipStatus.REQUESTED).build());

        perform(post("/api/friends/requests/" + req.getId() + "/accept"), c).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("하루 두 번째 명언 → 409")
    void secondQuoteSameDay_409() throws Exception {
        String body = "{\"content\":\"오늘도 좋은 하루를 보내자\",\"originalContent\":\"원문\",\"summary\":null,\"taggedMemberIds\":[]}";
        perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content(body), a)
                .andExpect(status().isCreated());
        perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content(body), a)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("날짜 형식 오류 → 400")
    void badDate_400() throws Exception {
        perform(get("/api/quotes").param("date", "bad"), a)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400"));
    }

    @Test
    @DisplayName("필수 파라미터 누락 → 400, 본문 파싱 오류 → 400, 405")
    void standardMvcErrors_4xx() throws Exception {
        perform(get("/api/quotes"), a).andExpect(status().isBadRequest());
        perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content("{bad"), a)
                .andExpect(status().isBadRequest());
        perform(put("/api/quotes/feed"), a).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("없는 명언 좋아요 → 404")
    void likeMissingQuote_404() throws Exception {
        perform(post("/api/quotes/999999/like"), a).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("남의 알림 읽음 처리 → 403, 읽음 처리되지 않음")
    void readOthersNotification_403() throws Exception {
        Notification n = notificationRepository.save(Notification.builder()
                .receiver(b).sender(a).type("POKE").message("콕").targetId(a.getId()).build());

        perform(patch("/api/notifications/" + n.getId() + "/read"), c).andExpect(status().isForbidden());
        assertThat(notificationRepository.findById(n.getId()).orElseThrow().getReadDate()).isNull();
    }

    @Test
    @DisplayName("없는 경로 → 404, 없는 공지 → 404")
    void notFound_404() throws Exception {
        perform(get("/api/no-such-path"), a).andExpect(status().isNotFound());
        perform(get("/api/notices/999999"), a).andExpect(status().isNotFound());
    }
}
