package com.ll.demo.global.aws;

import com.ll.demo.global.exceptions.GlobalException;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final String PROFILE_KEY_PREFIX = "profile/";

    // SVG는 스크립트를 담을 수 있어 제외
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/heic", "heic"
    );

    private final S3Template s3Template; // Spring Cloud AWS가 제공하는 도구

    // yml 파일 버킷 읽어오도록 임시 주석 해제
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    // 프로필 이미지 읽어오기 : 이미지 S3 객체 Key-Bucket Policy의 Resource 경로 일치하게 수정
    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "." + imageExtension(file.getContentType());

        s3Template.upload(bucket, PROFILE_KEY_PREFIX + fileName, file.getInputStream());

        return profileUrlPrefix() + fileName;
    }

    public String profileUrlPrefix() {
        return "https://%s.s3.ap-northeast-2.amazonaws.com/%s".formatted(bucket, PROFILE_KEY_PREFIX);
    }

    private String imageExtension(String contentType) {
        String mimeType = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase();
        String extension = IMAGE_EXTENSIONS.get(mimeType);
        if (extension == null) {
            throw new GlobalException("400", "이미지 파일만 업로드할 수 있습니다.");
        }
        return extension;
    }
}