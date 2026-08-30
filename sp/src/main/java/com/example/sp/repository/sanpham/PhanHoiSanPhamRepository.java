package com.example.sp.repository.sanpham;

import com.example.sp.model.sanpham.PhanHoiSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhanHoiSanPhamRepository extends JpaRepository<PhanHoiSanPham, Integer> {

    List<PhanHoiSanPham> findBySanPham_IdSpAndTrangThaiTrueOrderByNgayTaoDesc(Integer productId);

    List<PhanHoiSanPham> findAllByOrderByNgayTaoDesc();

    List<PhanHoiSanPham> findByKhachHang_IdOrderByNgayCapNhatDesc(Integer customerId);

    Optional<PhanHoiSanPham> findBySanPham_IdSpAndKhachHang_Id(Integer productId, Integer customerId);

    @Query("""
        SELECT p.diemDanhGia, COUNT(p)
        FROM PhanHoiSanPham p
        WHERE p.sanPham.idSp = :productId AND p.trangThai = true
        GROUP BY p.diemDanhGia
        """)
    List<Object[]> countByRatingForProduct(@Param("productId") Integer productId);
}
