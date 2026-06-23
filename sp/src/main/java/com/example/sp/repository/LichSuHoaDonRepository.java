package com.example.sp.repository;

import com.example.sp.model.order.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Long> {

    List<LichSuHoaDon> findByHoaDon_IdOrderByThoiGianDesc(Integer idHoaDon);
}