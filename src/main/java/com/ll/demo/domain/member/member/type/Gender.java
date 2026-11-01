package com.ll.demo.domain.member.member.type;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("남성"),
    FEMALE("여성"),
    ETC("선택하지 않음");

    private final String value;

    Gender(String value) {
        this.value = value;
    }
}