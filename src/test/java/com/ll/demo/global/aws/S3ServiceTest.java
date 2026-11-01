package com.ll.demo.global.aws;

import com.ll.demo.global.exceptions.GlobalException;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock private S3Template s3Template;
    @InjectMocks private S3Service s3Service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");
    }

    private MockMultipartFile file(String name, String contentType) {
        return new MockMultipartFile("image", name, contentType, new byte[]{1, 2, 3});
    }

    @ParameterizedTest
    @CsvSource({"image/png,png", "image/jpeg,jpg", "image/webp,webp"})
    @DisplayName("키는 profile/UUID.확장자이고 원본 파일명을 쓰지 않는다")
    void key_excludesOriginalFilename(String contentType, String extension) throws IOException {
        String url = s3Service.uploadFile(file("../비밀 사진.exe", contentType));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(s3Template).upload(eq("test-bucket"), key.capture(), any(InputStream.class));
        assertThat(key.getValue()).matches("profile/[0-9a-f-]{36}\\." + extension);
        assertThat(url).isEqualTo("https://test-bucket.s3.ap-northeast-2.amazonaws.com/" + key.getValue());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"text/plain", "application/pdf", "imagefake"})
    @DisplayName("이미지 MIME이 아니면 400, 업로드하지 않음")
    void nonImage_400(String contentType) {
        assertThatThrownBy(() -> s3Service.uploadFile(file("a.png", contentType)))
                .isInstanceOf(GlobalException.class)
                .satisfies(e -> assertThat(((GlobalException) e).getRsData().getStatusCode()).isEqualTo(400));
        verifyNoInteractions(s3Template);
    }

    @Test
    @DisplayName("파라미터가 붙은 MIME도 확장자만 안전하게 추출")
    void contentTypeWithParameter() throws IOException {
        s3Service.uploadFile(file("a.svg", "image/svg+xml; charset=utf-8"));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(s3Template).upload(eq("test-bucket"), key.capture(), any(InputStream.class));
        assertThat(key.getValue()).matches("profile/[0-9a-f-]{36}\\.svg");
    }
}
