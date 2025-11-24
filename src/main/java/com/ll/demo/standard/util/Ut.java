package com.ll.demo.standard.util;

// 문자열 유효 체크 유틸리티
public class Ut {
    public static class str {
        public static boolean isBlank(String str) {
            return str == null || str.isBlank();
        }

        public static boolean hasLength(String str) {
            return !isBlank(str);
        }
    }
}