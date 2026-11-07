package com.ll.demo.global.push;

import java.util.List;

public interface PushSender {

    // 같은 메시지를 여러 기기에 보내고, FCM이 무효라고 응답한 토큰을 돌려준다
    List<String> send(List<String> tokens, PushMessage message);
}
