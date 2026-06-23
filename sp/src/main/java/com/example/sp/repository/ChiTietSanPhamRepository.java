package com.example.sp.repository;

import com.example.sp.model.ChiTietSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
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
          AND COALESCE(ct.soLuongTon, 0) > 0
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
          AND COALESCE(ct.soLuongTon, 0) > 0
    """)
    List<ChiTietSanPham> findActiveSellableVariantsByProductId(@Param("idSp") Integer idSp);

}
