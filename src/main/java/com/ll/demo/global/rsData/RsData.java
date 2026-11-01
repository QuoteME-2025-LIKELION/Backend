package com.ll.demo.global.rsData;

// 응답 데이터
public class RsData<T> {

    private final String resultCode;
    private final String message;
    private final T data;

    private RsData(String resultCode, String message, T data) {
        this.resultCode = resultCode;
        this.message = message;
        this.data = data;
    }

    // 기존 방식 지원
    public static <T> RsData<T> of(String message, T data) {
        String resultCode = "200";

        if (message != null && message.length() >= 3) {
            resultCode = message.substring(0, 3);
        }

        return new RsData<>(resultCode, message, data);
    }

    // 새 방식 추가
    public static <T> RsData<T> of(String resultCode, String message, T data) {
        return new RsData<>(resultCode, message, data);
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public <U> RsData<U> newDataOf(U data) {
        return new RsData<>(resultCode, message, data);
    }

    public int getStatusCode() {
        if (resultCode == null || resultCode.isEmpty()) {
            return 200;
        }

        try {
            return Integer.parseInt(resultCode.substring(0, 3));
        } catch (Exception e) {
            return 200;
        }
    }
}