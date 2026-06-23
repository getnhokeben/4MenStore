package com.example.sp.repository;

import com.example.sp.model.XuatXu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface XuatXuRepository extends JpaRepository<XuatXu, Integer> {
    List<XuatXu> findByTenXuatXu(String tenXuatXu);
}