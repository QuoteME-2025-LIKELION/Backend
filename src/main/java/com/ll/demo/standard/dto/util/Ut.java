package com.ll.demo.standard.dto.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// 문자열 및 JSON 유효 체크
public class Ut {

    public static class json {
        private static final ObjectMapper om = new ObjectMapper();
        public static String toString(Object obj) {
            try {
                return om.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class str {
        public static boolean isBlank(String s) {
            return s == null || s.isBlank();
        }

        public static boolean isNotBlank(String s) {
            return s != null && !s.trim().isEmpty();
        }

        public static boolean hasLength(String s) {
            return !isBlank(s);
        }
    }
}