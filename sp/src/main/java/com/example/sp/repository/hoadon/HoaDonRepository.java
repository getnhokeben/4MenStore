package com.example.sp.repository.hoadon;

import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.ThanhToan;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository

public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {

    @Query("""
    SELECT COALESCE(MAX(h.tongTienThanhToan), 0)
    FROM HoaDon h
    """)
    BigDecimal findMaxTongTienThanhToan();

    @EntityGraph(attributePaths = {"nhanVien", "khachHang", "phieuGiamGia"})
    @Query("""
    SELECT h FROM HoaDon h
    WHERE (:maHD IS NULL OR h.maHoaDon LIKE CONCAT('%', :maHD, '%'))
      AND (:tuNgay IS NULL OR h.ngayTao >= :tuNgay)
      AND (:denNgay IS NULL OR h.ngayTao <= :denNgay)
      AND (:loaiDon IS NULL OR h.loaiDon = :loaiDon
           OR (:loaiDon = 'Tại quầy' AND h.loaiDon = 'Giao hàng'))
      AND (:trangThai IS NULL OR h.trangThai IN :dsTrangThai)
      AND (:maxGia IS NULL OR h.tongTienThanhToan <= :maxGia)
    ORDER BY h.ngayTao DESC
    """)

    Page<HoaDon> timKiem(
            @Param("maHD") String maHD,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay,
            @Param("loaiDon") String loaiDon,
            @Param("trangThai") String trangThai,
            @Param("dsTrangThai") List<String> dsTrangThai,
            @Param("maxGia") BigDecimal maxGia,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"nhanVien", "khachHang", "phieuGiamGia"})
    java.util.Optional<HoaDon> findWithRelationsById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HoaDon h WHERE h.id = :id")
    java.util.Optional<HoaDon> findByIdForUpdate(@Param("id") Integer id);

    long countByLoaiDonAndTrangThai(String loaiDon, String trangThai);

    boolean existsByMaHoaDon(String maHoaDon);

    @Query("""
            SELECT COUNT(h)
            FROM HoaDon h
            WHERE h.phieuGiamGia.id = :voucherId
              AND h.khachHang.id = :customerId
              AND (h.trangThai IS NULL OR h.trangThai NOT IN :cancelledStatuses)
            """)
    long countActiveUsesByVoucherAndCustomer(
            @Param("voucherId") Integer voucherId,
            @Param("customerId") Integer customerId,
            @Param("cancelledStatuses") List<String> cancelledStatuses
    );

    List<HoaDon> findByLoaiDonAndTrangThaiOrderByNgayTaoAsc(String loaiDon, String trangThai);

    /**
     * Online orders that have already consumed stock but have not begun
     * packing/shipping may be moved back to waiting for restock when a customer
     * is paying at the counter.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT DISTINCT h
            FROM HoaDonChiTiet c
            JOIN c.hoaDon h
            WHERE c.chiTietSanPham.idSpct = :variantId
              AND h.loaiDon = 'Trực tuyến'
              AND h.trangThai = 'Đã xác nhận'
              AND h.daTruTon = true
            ORDER BY h.ngayTao ASC
            """)
    List<HoaDon> findOnlineOrdersPreemptibleForCounterSale(@Param("variantId") Integer variantId);

    @EntityGraph(attributePaths = {"phieuGiamGia", "khachHang"})
    List<HoaDon> findByKhachHang_IdOrderByNgayTaoDesc(Integer idKhachHang);

    @EntityGraph(attributePaths = {"phieuGiamGia", "khachHang"})
    java.util.Optional<HoaDon> findFirstByMaHoaDonIgnoreCase(String maHoaDon);

    @Query("SELECT t FROM ThanhToan t JOIN FETCH t.phuongThucThanhToan WHERE t.hoaDon.id = :hoaDonId")
    List<ThanhToan> findByHoaDonIdWithPaymentMethod(@Param("hoaDonId") Long hoaDonId);
}
