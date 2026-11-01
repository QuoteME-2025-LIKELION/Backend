package com.ll.demo.domain.profile.profile.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.global.aws.S3Service;
import com.ll.demo.global.security.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GuestAccountUpdateTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AuthTokenService authTokenService;
    @MockBean private S3Service s3Service;

    private Member guest;
    private Member member;

    @BeforeEach
    void setUp() {
        guest = memberRepository.findByEmail("guest@guest.com").orElseGet(() -> memberRepository.save(
                Member.builder().email("guest@guest.com").nickname("게스트").password("password123").build()));
        member = memberRepository.save(Member.builder().email("real@test.com").nickname("회원").password("password123").build());
    }

    private ResultActions updateAccount(Member as, String email) throws Exception {
        return mvc.perform(put("/api/profile/account")
                .header("Authorization", "Bearer " + authTokenService.genToken(as, 600))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"birthYear\":\"2000\",\"gender\":\"MALE\"}"));
    }

    private ResultActions updateProfile(Member as) throws Exception {
        MockMultipartFile data = new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"nickname\":\"새이름\"}".getBytes());
        MockMultipartFile image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1});
        return mvc.perform(multipart("/api/profile").file(data).file(image)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .header("Authorization", "Bearer " + authTokenService.genToken(as, 600)));
    }

    @Test
    @DisplayName("게스트 계정 정보 수정 → 403")
    void guestAccount_403() throws Exception {
        updateAccount(guest, "guest@guest.com").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게스트 프로필 수정 → 403, 이미지도 업로드하지 않음")
    void guestProfile_403() throws Exception {
        updateProfile(guest).andExpect(status().isForbidden());

        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("일반 회원은 영향 없음")
    void normalMember_ok() throws Exception {
        updateAccount(member, "real@test.com").andExpect(status().isOk());
        updateProfile(member).andExpect(status().isOk());
    }
}
