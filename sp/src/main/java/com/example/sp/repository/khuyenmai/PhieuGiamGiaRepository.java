package com.example.sp.repository.khuyenmai;

import com.example.sp.model.khuyenmai.PhieuGiamGia;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Integer> {

    @Query("""
        SELECT p FROM PhieuGiamGia p
        WHERE (:keyword IS NULL OR p.maPgg LIKE CONCAT('%', :keyword, '%') OR p.tenPgg LIKE CONCAT('%', :keyword, '%'))
          AND (:trangThai IS NULL OR p.trangThai = :trangThai)
          AND (:tienDo IS NULL
               OR (:tienDo = 'SAP_DIEN_RA' AND p.trangThai = true AND p.ngayBatDau > :now AND p.ngayKetThuc > :now)
               OR (:tienDo IN ('DANG_DIEN_RA', 'DANG_AP_DUNG') AND p.trangThai = true AND p.ngayBatDau <= :now AND p.ngayKetThuc >= :now)
               OR (:tienDo = 'KET_THUC' AND (p.trangThai = false OR p.ngayKetThuc < :now)))
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PhieuGiamGia p WHERE p.id = :id")
    Optional<PhieuGiamGia> findByIdForUpdate(@Param("id") Integer id);

    List<PhieuGiamGia> findByTrangThaiTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE PhieuGiamGia p SET p.trangThai = false "
            + "WHERE p.trangThai = true "
            + "AND p.ngayKetThuc IS NOT NULL "
            + "AND p.ngayKetThuc <= :now")
    int deactivateExpiredVouchers(@Param("now") LocalDateTime now);

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
