package com.ll.demo.global.push;

import java.util.Map;

public record PushMessage(String title, String body, Map<String, String> data) { }
