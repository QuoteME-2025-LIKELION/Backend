package com.ll.demo.domain.quote.dto;

import java.util.List;

public class QuoteSummarizeResponse {
    private List<String> summaries;

    public QuoteSummarizeResponse() {
    }

    public QuoteSummarizeResponse(List<String> summaries) {
        this.summaries = summaries;
    }

    public List<String> getSummaries() {
        return summaries;
    }

    public void setSummaries(List<String> summaries) {
        this.summaries = summaries;
    }
}
