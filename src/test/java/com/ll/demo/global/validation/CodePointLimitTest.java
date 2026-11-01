package com.ll.demo.global.validation;

import com.ll.demo.domain.group.group.dto.GroupRequest;
import com.ll.demo.domain.group.group.dto.MottoRequest;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.profile.profile.dto.ProfileUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CodePointLimitTest {

    private static final String EMOJI = "😀";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean validGroupName(String name) {
        return validator.validate(new GroupRequest(name, null)).isEmpty();
    }

    private boolean validNickname(String nickname) {
        return validator.validate(new ProfileUpdateRequest(nickname, null, null)).isEmpty();
    }

    @Test
    @DisplayName("닉네임은 코드포인트 10자까지")
    void nickname() {
        assertThat(validNickname(EMOJI.repeat(10))).isTrue();
        assertThat(validNickname(EMOJI.repeat(11))).isFalse();
        assertThat(validNickname("a".repeat(10))).isTrue();
        assertThat(validNickname("a".repeat(11))).isFalse();
    }

    @Test
    @DisplayName("자기소개는 코드포인트 20자까지")
    void introduction() {
        assertThat(validator.validate(new ProfileUpdateRequest(null, EMOJI.repeat(20), null))).isEmpty();
        assertThat(validator.validate(new ProfileUpdateRequest(null, EMOJI.repeat(21), null))).isNotEmpty();
    }

    @Test
    @DisplayName("그룹 메시지는 코드포인트 20자까지")
    void groupMessage() {
        assertThat(validator.validate(new MottoRequest(EMOJI.repeat(20)))).isEmpty();
        assertThat(validator.validate(new MottoRequest(EMOJI.repeat(21)))).isNotEmpty();
        assertThat(validator.validate(new GroupRequest("그룹", EMOJI.repeat(20)))).isEmpty();
        assertThat(validator.validate(new GroupRequest("그룹", EMOJI.repeat(21)))).isNotEmpty();
    }

    @Test
    @DisplayName("Member 엔티티도 같은 기준으로 영속화를 허용한다")
    void memberEntity() {
        Member ok = Member.builder().nickname(EMOJI.repeat(10)).introduction(EMOJI.repeat(20)).build();
        Member tooLong = Member.builder().nickname(EMOJI.repeat(11)).introduction(EMOJI.repeat(21)).build();

        assertThat(validator.validateProperty(ok, "nickname")).isEmpty();
        assertThat(validator.validateProperty(ok, "introduction")).isEmpty();
        assertThat(validator.validateProperty(tooLong, "nickname")).isNotEmpty();
        assertThat(validator.validateProperty(tooLong, "introduction")).isNotEmpty();
    }

    @Test
    @DisplayName("그룹명은 코드포인트 10자까지")
    void groupName_length() {
        assertThat(validGroupName(EMOJI.repeat(10))).isTrue();
        assertThat(validGroupName(EMOJI.repeat(11))).isFalse();
        assertThat(validGroupName("a".repeat(10))).isTrue();
        assertThat(validGroupName("a".repeat(11))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"우리 모임😀", "Team 1", "👨‍👩‍👧", "👍🏽", "❤️", "모임123"})
    @DisplayName("그룹명 허용 문자: 한글·영문·숫자·공백·이모지")
    void groupName_allowed(String name) {
        assertThat(validGroupName(name)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"우리!모임", "#", "*", "a_b", "모임#1", "ㅎ.ㅎ"})
    @DisplayName("그룹명 허용되지 않는 문자")
    void groupName_rejected(String name) {
        assertThat(validGroupName(name)).isFalse();
    }
}
