package com.example.sp.repository.sanpham;

import com.example.sp.model.sanpham.XuatXu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface XuatXuRepository extends JpaRepository<XuatXu, Integer> {
    List<XuatXu> findByTenXuatXu(String tenXuatXu);
}