package com.ll.demo.domain.group.group.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.group.group.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    //  그룹 자체를 조회
    List<Group> findByNameContainingIgnoreCase(String nameKeyword);

    List<Group> findAllByLeader(Member leader);

    // 그룹명 중복: 앞뒤 공백 제거 + 대소문자 무시
    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE LOWER(TRIM(g.name)) = LOWER(TRIM(:name))")
    boolean existsByNormalizedName(@Param("name") String name);
}