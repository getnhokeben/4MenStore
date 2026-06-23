package com.example.sp.repository;

import com.example.sp.model.order.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {

    @Query("""
    SELECT h FROM HoaDon h
    WHERE (:maHD IS NULL OR h.maHoaDon LIKE CONCAT('%', :maHD, '%'))
      AND (:tuNgay IS NULL OR h.ngayTao >= :tuNgay)
      AND (:denNgay IS NULL OR h.ngayTao <= :denNgay)
      AND (:loaiDon IS NULL OR h.loaiDon = :loaiDon)
      AND (:trangThai IS NULL OR h.trangThai = :trangThai)
    ORDER BY h.ngayTao DESC
    """)
    Page<HoaDon> timKiem(
            @Param("maHD") String maHD,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay,
            @Param("loaiDon") String loaiDon,
            @Param("trangThai") String trangThai,
            Pageable pageable
    );
}