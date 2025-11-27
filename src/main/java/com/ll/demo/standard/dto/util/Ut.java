package com.ll.demo.standard.dto.util;

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

    // SecurityConfig에서 호출하는 toString
    public static String toString(Object obj) {
        //
        return "{}";
    }

    public static class json {

        public static String toString(Object obj) {
            //
            return "{}";
        }
    }
}