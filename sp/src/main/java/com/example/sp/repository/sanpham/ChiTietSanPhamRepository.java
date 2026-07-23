package com.example.sp.repository.sanpham;

import com.example.sp.model.sanpham.ChiTietSanPham;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, Integer> {
    List<ChiTietSanPham> findByIdSanPham(Integer idSanPham);
    boolean existsByMaChiTietSanPham(String maChiTietSanPham);
    Optional<ChiTietSanPham> findByMaChiTietSanPham(String maChiTietSanPham);

    @Query("""
        SELECT ct FROM ChiTietSanPham ct
        JOIN FETCH ct.sanPham sp
        LEFT JOIN FETCH sp.chatLieu
        LEFT JOIN FETCH sp.xuatXu
        LEFT JOIN FETCH ct.kichCo
        LEFT JOIN FETCH ct.mauSac
        LEFT JOIN FETCH ct.loaiAo
        LEFT JOIN FETCH ct.phongCachMac
        LEFT JOIN FETCH ct.kieuDang
        WHERE sp.trangThai = true
          AND ct.trangThai = true
          AND COALESCE(ct.soLuongTon, 0) - COALESCE(ct.soLuongGiu, 0) > 0
    """)
    List<ChiTietSanPham> findActiveSellableVariants();

    @Query("""
        SELECT ct FROM ChiTietSanPham ct
        JOIN FETCH ct.sanPham sp
        LEFT JOIN FETCH sp.chatLieu
        LEFT JOIN FETCH sp.xuatXu
        LEFT JOIN FETCH ct.kichCo
        LEFT JOIN FETCH ct.mauSac
        LEFT JOIN FETCH ct.loaiAo
        LEFT JOIN FETCH ct.phongCachMac
        LEFT JOIN FETCH ct.kieuDang
        WHERE sp.idSp = :idSp
          AND sp.trangThai = true
          AND ct.trangThai = true
          AND COALESCE(ct.soLuongTon, 0) - COALESCE(ct.soLuongGiu, 0) > 0
    """)
    List<ChiTietSanPham> findActiveSellableVariantsByProductId(@Param("idSp") Integer idSp);

    @Query("""
        SELECT CASE WHEN COUNT(ct) > 0 THEN true ELSE false END
        FROM ChiTietSanPham ct
        JOIN ct.sanPham sp
        WHERE ct.idSpct = :idSpct
          AND ct.trangThai = true
          AND sp.trangThai = true
          AND COALESCE(ct.soLuongTon, 0) - COALESCE(ct.soLuongGiu, 0) > 0
    """)
    boolean existsActiveVariantById(@Param("idSpct") Integer idSpct);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ct FROM ChiTietSanPham ct
        LEFT JOIN FETCH ct.sanPham sp
        WHERE ct.idSpct = :idSpct
    """)
    Optional<ChiTietSanPham> findByIdForUpdate(@Param("idSpct") Integer idSpct);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ct FROM ChiTietSanPham ct WHERE ct.idSanPham = :idSanPham")
    List<ChiTietSanPham> findByIdSanPhamForUpdate(@Param("idSanPham") Integer idSanPham);

}
