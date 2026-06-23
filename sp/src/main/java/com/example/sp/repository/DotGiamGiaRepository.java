package com.example.sp.repository;

import com.example.sp.dto.promotion.SanPhamChiTietPromotionView;
import com.example.sp.model.promotion.DotGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Integer> {

    @Query("""
        SELECT d FROM DotGiamGia d
        WHERE (:keyword IS NULL OR d.maDotGiamGia LIKE CONCAT('%', :keyword, '%') OR d.tenDotGiamGia LIKE CONCAT('%', :keyword, '%'))
          AND (:trangThai IS NULL OR d.trangThai = :trangThai)
          AND (:tienDo IS NULL
               OR (:tienDo IN ('DANG_HOAT_DONG', 'DANG_DIEN_RA') AND d.trangThai = true AND d.ngayKetThuc >= :now)
               OR (:tienDo IN ('NGUNG_HOAT_DONG', 'SAP_DIEN_RA') AND d.trangThai = false AND d.ngayKetThuc >= :now)
               OR (:tienDo IN ('HET_HAN', 'KET_THUC') AND d.ngayKetThuc < :now))
          AND (:tuNgay IS NULL OR d.ngayBatDau >= :tuNgay)
          AND (:denNgay IS NULL OR d.ngayKetThuc <= :denNgay)
    """)
    Page<DotGiamGia> search(
            @Param("keyword") String keyword,
            @Param("trangThai") Boolean trangThai,
            @Param("tienDo") String tienDo,
            @Param("now") LocalDateTime now,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay,
            Pageable pageable
    );

    boolean existsByMaDotGiamGiaAndIdNot(String maDotGiamGia, Integer id);

    @Modifying
    @Query("UPDATE DotGiamGia d SET d.trangThai = :trangThai WHERE d.id = :id")
    void updateTrangThaiById(@Param("id") Integer id, @Param("trangThai") Boolean trangThai);

    @Modifying
    @Query("UPDATE DotGiamGia d SET d.trangThai = false, d.ngayKetThuc = :ngayKetThuc WHERE d.id = :id")
    void finishById(@Param("id") Integer id, @Param("ngayKetThuc") LocalDateTime ngayKetThuc);

    @Modifying
    @Query("UPDATE DotGiamGia d SET d.trangThai = true, d.ngayBatDau = :ngayBatDau, d.ngayKetThuc = :ngayKetThuc WHERE d.id = :id")
    void activateById(
            @Param("id") Integer id,
            @Param("ngayBatDau") LocalDateTime ngayBatDau,
            @Param("ngayKetThuc") LocalDateTime ngayKetThuc
    );

    @Query(value = """
        SELECT ct.id_spct AS idSpct,
               ct.ma_chi_tiet_san_pham AS maSpct,
               sp.ten_sp AS tenSp,
               ms.ten_mau_sac AS tenMauSac,
               kc.ten_kich_co AS tenKichCo,
               ct.gia_ban AS giaBan,
               ct.so_luong_ton AS soLuongTon
        FROM chi_tiet_san_pham ct
        INNER JOIN san_pham sp ON ct.id_san_pham = sp.id_sp
        LEFT JOIN mau_sac ms ON ct.id_mau_sac = ms.id_mau_sac
        LEFT JOIN kich_co kc ON ct.id_kich_co = kc.id_kich_co
        WHERE ct.trang_thai = 1
        ORDER BY sp.ten_sp, ms.ten_mau_sac, kc.ten_kich_co, ct.ma_chi_tiet_san_pham
    """, nativeQuery = true)
    List<SanPhamChiTietPromotionView> findSanPhamChiTietKichHoat();
}
