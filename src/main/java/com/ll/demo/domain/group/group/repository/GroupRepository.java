package com.ll.demo.domain.group.group.repository;

import com.ll.demo.domain.group.group.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {}