package com.ll.demo.global.push;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FcmPushSender implements PushSender {
    static final int MULTICAST_LIMIT = 500;
    private static final Set<MessagingErrorCode> INVALID_TOKEN_CODES =
            Set.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT);

    private final FirebaseMessaging messaging;

    public FcmPushSender(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public List<String> send(List<String> tokens, PushMessage message) {
        List<String> invalidTokens = new ArrayList<>();
        for (int from = 0; from < tokens.size(); from += MULTICAST_LIMIT) {
            List<String> chunk = tokens.subList(from, Math.min(from + MULTICAST_LIMIT, tokens.size()));
            List<SendResponse> responses = sendChunk(chunk, message).getResponses();
            for (int i = 0; i < responses.size(); i++) {
                FirebaseMessagingException e = responses.get(i).getException();
                if (e != null && INVALID_TOKEN_CODES.contains(e.getMessagingErrorCode())) {
                    invalidTokens.add(chunk.get(i));
                }
            }
        }
        return invalidTokens;
    }

    private BatchResponse sendChunk(List<String> chunk, PushMessage message) {
        MulticastMessage multicast = MulticastMessage.builder()
                .addAllTokens(chunk)
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.data())
                .build();
        try {
            return messaging.sendEachForMulticast(multicast);
        } catch (FirebaseMessagingException e) {
            throw new IllegalStateException("FCM 발송 실패: " + e.getMessagingErrorCode(), e);
        }
    }
}
