package com.ll.demo.global.rsData;

// 응답 데이터
public class RsData<T> {

    private final String message;
    private final T data;

    private RsData(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public static <T> RsData<T> of(String message, T data) {
        return new RsData<>(message, data);
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}