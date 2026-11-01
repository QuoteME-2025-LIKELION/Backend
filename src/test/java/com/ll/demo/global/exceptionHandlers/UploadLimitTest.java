package com.ll.demo.global.exceptionHandlers;

import com.ll.demo.global.rsData.RsData;
import com.ll.demo.standard.dto.Empty;
import com.ll.demo.standard.rq.Rq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UploadLimitTest {

    @Test
    @DisplayName("application.yml에 업로드 한도가 5MB/6MB로 설정되어 있다")
    void limitsConfigured() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yml"));
        Properties properties = yaml.getObject();

        assertThat(properties.getProperty("spring.servlet.multipart.max-file-size")).isEqualTo("5MB");
        assertThat(properties.getProperty("spring.servlet.multipart.max-request-size")).isEqualTo("6MB");
    }

    @Test
    @DisplayName("업로드 초과 예외 → 413")
    void exceeded_413() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(mock(Rq.class));

        ResponseEntity<RsData<Empty>> response = handler.handleSpringMvcException(new MaxUploadSizeExceededException(5_242_880));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }
}
