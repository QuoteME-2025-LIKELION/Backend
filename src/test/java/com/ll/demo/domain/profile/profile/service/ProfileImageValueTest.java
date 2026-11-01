package com.ll.demo.domain.profile.profile.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.profile.profile.dto.ProfileUpdateRequest;
import com.ll.demo.global.exceptions.GlobalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProfileImageValueTest {

    private static final String BUCKET_PREFIX = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/";
    private static final String ORIGINAL = BUCKET_PREFIX + "original.png";

    @Autowired private ProfileService profileService;
    @Autowired private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder().email("image@test.com").nickname("이미지").password("password123")
                .profileImage(ORIGINAL).build());
    }

    private String update(String profileImage) {
        profileService.updateProfile(member, new ProfileUpdateRequest(null, null, profileImage), null);
        return memberRepository.findById(member.getId()).orElseThrow().getProfileImage();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.example/a.png",
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com.evil.example/profile/a.png",
            "https://other-bucket.s3.ap-northeast-2.amazonaws.com/profile/a.png",
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/private/a.png",
            "javascript:alert(1)",
            " "
    })
    @DisplayName("버킷 URL이 아닌 값 → 400, 변경 없음")
    void externalValue_400(String value) {
        assertThatThrownBy(() -> update(value))
                .isInstanceOf(GlobalException.class)
                .satisfies(e -> assertThat(((GlobalException) e).getRsData().getStatusCode()).isEqualTo(400));
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getProfileImage()).isEqualTo(ORIGINAL);
    }

    @Test
    @DisplayName("버킷 URL → 성공")
    void bucketUrl_ok() {
        assertThat(update(BUCKET_PREFIX + "new.png")).isEqualTo(BUCKET_PREFIX + "new.png");
    }

    @Test
    @DisplayName("null → 변경 없음")
    void null_unchanged() {
        assertThat(update(null)).isEqualTo(ORIGINAL);
    }

    @Test
    @DisplayName("빈 문자열 → 기존 동작대로 이미지 제거")
    void empty_clears() {
        assertThat(update("")).isEmpty();
    }

    @Test
    @DisplayName("업로드 파일이 있으면 JSON 값은 검증·적용하지 않는다")
    void uploadedImage_wins() {
        String uploaded = BUCKET_PREFIX + "uploaded.png";

        profileService.updateProfile(member, new ProfileUpdateRequest(null, null, "https://evil.example/a.png"), uploaded);

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getProfileImage()).isEqualTo(uploaded);
    }
}
