package com.ll.demo.global.aws;

import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template; // Spring Cloud AWS가 제공하는 도구

    //@Value("${spring.cloud.aws.s3.bucket}")
    //private String bucket;

    private String bucket = "quoteme-likelion-bucket";
    public String uploadFile(MultipartFile file) throws IOException {
        // 1. 파일 이름 중복 방지를 위해 UUID 생성
        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

        // 2. S3에 파일 업로드 (InputStream 방식)
        s3Template.upload(bucket, uniqueFilename, file.getInputStream());

        // 3. 업로드된 파일의 URL 리턴
        // (주의: 버킷이 public 읽기 권한이 있어야 외부에서 보입니다)
        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucket, uniqueFilename);
    }
}