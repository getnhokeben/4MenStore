package com.example.sp.repository;

import com.example.sp.model.LoaiAo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoaiAoRepository extends JpaRepository<LoaiAo, Integer> {
    Page<LoaiAo> findByTenLoaiContainingIgnoreCaseOrMaLoaiContainingIgnoreCase(String ten, String ma, Pageable pageable);
}