package com.ll.demo.domain.member.member.dto;
import com.ll.demo.domain.member.member.entity.Member;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberJoinRespBody {

    private MemberDto item;
    private String accessToken;
    // 앱 요청(X-Client-Type: app)에만 채움, 웹 응답에는 키가 없음
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshToken;

    public MemberJoinRespBody(MemberDto item, String accessToken) {
        this(item, accessToken, null);
    }
}