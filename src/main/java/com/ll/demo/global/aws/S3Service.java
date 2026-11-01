package com.ll.demo.global.aws;

import com.ll.demo.global.exceptions.GlobalException;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Pattern IMAGE_MIME = Pattern.compile("image/([a-zA-Z0-9.-]+)");

    private final S3Template s3Template; // Spring Cloud AWS가 제공하는 도구

    // yml 파일 버킷 읽어오도록 임시 주석 해제
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    // 프로필 이미지 읽어오기 : 이미지 S3 객체 Key-Bucket Policy의 Resource 경로 일치하게 수정
    public String uploadFile(MultipartFile file) throws IOException {
        String uniqueFilename = "profile/" + UUID.randomUUID() + "." + imageExtension(file.getContentType());

        s3Template.upload(bucket, uniqueFilename, file.getInputStream());

        return String.format(
                "https://%s.s3.ap-northeast-2.amazonaws.com/%s",
                bucket,
                uniqueFilename
        );
    }

    private String imageExtension(String contentType) {
        Matcher matcher = contentType == null ? null : IMAGE_MIME.matcher(contentType);
        if (matcher == null || !matcher.lookingAt()) {
            throw new GlobalException("400", "이미지 파일만 업로드할 수 있습니다.");
        }

        String subtype = matcher.group(1).toLowerCase();
        return subtype.equals("jpeg") ? "jpg" : subtype;
    }
}