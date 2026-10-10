package com.ll.demo.global.aws;

import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template; // Spring Cloud AWS가 제공하는 도구

    // yml 파일 버킷 읽어오도록 임시 주석 해제
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    // 프로필 이미지 읽어오기 : 이미지 S3 객체 Key-Bucket Policy의 Resource 경로 일치하게 수정
    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = "profile/" + UUID.randomUUID() + "_" + originalFilename;

        s3Template.upload(bucket, uniqueFilename, file.getInputStream());

        return String.format(
                "https://%s.s3.ap-northeast-2.amazonaws.com/%s",
                bucket,
                uniqueFilename
        );
    }
}