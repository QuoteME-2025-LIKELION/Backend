package com.ll.demo.domain.member.member.dto;

import com.ll.demo.domain.group.group.dto.GroupSearchResponse;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class SearchCombinedResponse {
    private List<MemberSearchResponse> members;
    private List<GroupSearchResponse> groups;
}