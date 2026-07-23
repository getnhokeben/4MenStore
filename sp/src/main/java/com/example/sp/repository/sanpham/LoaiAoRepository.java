package com.example.sp.repository.sanpham;

import com.example.sp.model.sanpham.LoaiAo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoaiAoRepository extends JpaRepository<LoaiAo, Integer> {
    Page<LoaiAo> findByTenLoaiContainingIgnoreCaseOrMaLoaiContainingIgnoreCase(String ten, String ma, Pageable pageable);
}