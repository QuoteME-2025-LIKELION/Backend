package com.ll.demo.global.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final Client client;

    // 생성자에서 API 키를 받아 클라이언트를 초기화합니다.
    public GeminiService(@Value("${custom.gemini.api-key}") String apiKey) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    public String summarize(String originalText) {
        String prompt = """
                역할: 너는 사람들의 평범한 일상을 통찰력 있는 '한 줄 명언'으로 바꿔주는 시인이자 철학자야.
                지시사항:
                1. 사용자가 입력한 내용을 읽고 핵심 감정이나 상황을 파악해.
                2. 그 내용을 바탕으로 짧고 강렬한 한국어 명언(Aphorism) 스타일로 변환해줘.
                3. 문체는 "~하다", "~이다" 처럼 담백하고 무게감 있게 끝내줘.
                4. 부가 설명 없이 오직 변환된 명언 한 문장만 출력해.
                
                입력 내용:
                """ + originalText;

        try {
            // 공식 라이브러리 사용 (모델명은 기존에 쓰던 1.5 flash 유지)
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    prompt,
                    null
            );

            // 응답에서 텍스트만 추출
            return response.text();

        } catch (Exception e) {
            e.printStackTrace(); // 에러 로그 출력
            return "AI 요약을 이용할 수 없습니다.";
        }
    }
}