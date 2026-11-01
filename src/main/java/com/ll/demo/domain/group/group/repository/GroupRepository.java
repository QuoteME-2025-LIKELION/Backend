package com.ll.demo.domain.group.group.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.group.group.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    //  그룹 자체를 조회
    List<Group> findByNameContainingIgnoreCase(String nameKeyword);

    List<Group> findAllByLeader(Member leader);
}