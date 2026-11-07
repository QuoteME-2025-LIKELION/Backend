package com.ll.demo.global.push;

import java.util.List;

// 자격 증명이 없을 때 사용 (푸시만 꺼지고 인앱 알림은 그대로)
public class NoOpPushSender implements PushSender {
    @Override
    public List<String> send(List<String> tokens, PushMessage message) {
        return List.of();
    }
}
