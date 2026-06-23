package com.example.sp.repository;

import com.example.sp.model.promotion.PhieuGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Integer> {

    @Query("""
        SELECT p FROM PhieuGiamGia p
        WHERE (:keyword IS NULL OR p.maPgg LIKE CONCAT('%', :keyword, '%') OR p.tenPgg LIKE CONCAT('%', :keyword, '%'))
          AND (:trangThai IS NULL OR p.trangThai = :trangThai)
          AND (:tienDo IS NULL
               OR (:tienDo = 'SAP_DIEN_RA' AND p.trangThai = false AND p.ngayBatDau > :now)
               OR (:tienDo IN ('DANG_DIEN_RA', 'DANG_AP_DUNG') AND p.trangThai = true)
               OR (:tienDo = 'KET_THUC' AND (p.ngayKetThuc < :now OR (p.trangThai = false AND p.ngayBatDau <= :now))))
          AND (:loaiGiam IS NULL OR p.loaiGiam = :loaiGiam)
          AND (:tuNgay IS NULL OR p.ngayBatDau >= :tuNgay)
          AND (:denNgay IS NULL OR p.ngayKetThuc <= :denNgay)
    """)
    Page<PhieuGiamGia> search(
            @Param("keyword") String keyword,
            @Param("loaiGiam") String loaiGiam,
            @Param("trangThai") Boolean trangThai,
            @Param("tienDo") String tienDo,
            @Param("now") LocalDateTime now,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay,
            Pageable pageable
    );

    boolean existsByMaPggAndIdNot(String maPgg, Integer id);

    Optional<PhieuGiamGia> findFirstByMaPggIgnoreCase(String maPgg);

    @Modifying
    @Query("UPDATE PhieuGiamGia p SET p.trangThai = false WHERE p.id = :id")
    void softDeleteById(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE PhieuGiamGia p SET p.trangThai = :trangThai WHERE p.id = :id")
    void updateTrangThaiById(@Param("id") Integer id, @Param("trangThai") Boolean trangThai);

    @Modifying
    @Query("UPDATE PhieuGiamGia p SET p.trangThai = true, p.ngayBatDau = :ngayBatDau, p.ngayKetThuc = :ngayKetThuc WHERE p.id = :id")
    void activateById(@Param("id") Integer id, @Param("ngayBatDau") LocalDateTime ngayBatDau, @Param("ngayKetThuc") LocalDateTime ngayKetThuc);

    @Modifying
    @Query("UPDATE PhieuGiamGia p SET p.trangThai = false, p.ngayKetThuc = :ngayKetThuc WHERE p.id = :id")
    void finishById(@Param("id") Integer id, @Param("ngayKetThuc") LocalDateTime ngayKetThuc);
}
