package com.ll.demo.global;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DuplicateCheckSqlTest {

    @Autowired private EntityManager em;

    @Test
    @DisplayName("점검 SQL의 테이블·컬럼 이름이 Hibernate 스키마와 일치한다")
    void selects_matchSchema() throws IOException {
        String withoutComments = String.join("\n", Files.readAllLines(Path.of("scripts/duplicate-check.sql")).stream()
                .filter(line -> !line.startsWith("--"))
                .toList());

        List<String> selects = Arrays.stream(withoutComments.split(";"))
                .map(String::strip)
                .filter(sql -> !sql.isEmpty())
                .toList();

        assertThat(selects).hasSize(5).allMatch(sql -> sql.startsWith("SELECT"));
        selects.forEach(sql -> assertThat(em.createNativeQuery(sql).getResultList()).isEmpty());
    }
}
