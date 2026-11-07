package com.ll.demo.global.push;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FcmPushSenderTest {

    private static final PushMessage MESSAGE = new PushMessage("QuoteME", "본문", Map.of("type", "POKE"));

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FcmPushSender sender = new FcmPushSender(messaging);

    private static SendResponse ok() {
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(true);
        return response;
    }

    private static SendResponse failed(MessagingErrorCode code) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(code);
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(false);
        given(response.getException()).willReturn(exception);
        return response;
    }

    private static BatchResponse batch(List<SendResponse> responses) {
        BatchResponse batch = mock(BatchResponse.class);
        given(batch.getResponses()).willReturn(responses);
        return batch;
    }

    @Test
    @DisplayName("UNREGISTERED·INVALID_ARGUMENT 토큰만 무효로 돌려준다")
    void returnsInvalidTokens() throws Exception {
        BatchResponse response = batch(List.of(
                ok(),
                failed(MessagingErrorCode.UNREGISTERED),
                failed(MessagingErrorCode.INVALID_ARGUMENT),
                failed(MessagingErrorCode.UNAVAILABLE)));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        List<String> invalid = sender.send(List.of("ok", "gone", "bad", "retry-later"), MESSAGE);

        assertThat(invalid).containsExactly("gone", "bad");
    }

    @Test
    @DisplayName("500개 단위로 나눠 멀티캐스트, 뒤쪽 묶음의 무효 토큰도 정확히 찾는다")
    void chunksBy500() throws Exception {
        List<String> tokens = IntStream.range(0, 501).mapToObj(i -> "t" + i).toList();
        List<SendResponse> first = new ArrayList<>(IntStream.range(0, 500).mapToObj(i -> ok()).toList());
        BatchResponse firstBatch = batch(first);
        BatchResponse secondBatch = batch(List.of(failed(MessagingErrorCode.UNREGISTERED)));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(firstBatch, secondBatch);

        List<String> invalid = sender.send(tokens, MESSAGE);

        verify(messaging, times(2)).sendEachForMulticast(any(MulticastMessage.class));
        assertThat(invalid).containsExactly("t500");
    }
}
