package com.ll.demo.domain.quote.dto;

import java.util.List;

public record QuoteTagUpdateReq(
        List<Long> taggedMemberIds // 태그된 친구들의 ID 리스트 (예: [3, 15, 22])
) {}