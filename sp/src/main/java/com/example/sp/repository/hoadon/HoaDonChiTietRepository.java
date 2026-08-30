package com.example.sp.repository.hoadon;

import com.example.sp.dto.hoadon.HoaDonChiTietDTO;
import com.example.sp.model.hoadon.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    List<HoaDonChiTiet> findByHoaDon_Id(Integer idHoaDon);

    /**
     * Lấy danh sách sản phẩm trong 1 hóa đơn, JOIN đủ các bảng lookup.
     * Map thẳng vào HoaDonChiTietDTO qua JPQL constructor expression.
     */
    @Query("""
        SELECT new com.example.sp.dto.hoadon.HoaDonChiTietDTO(
            ct.id,
            spct.idSpct,
            sp.maSp,
            sp.tenSp,
            ms.tenMauSac,
            kc.tenKichCo,
            kd.tenKieuDang,
            la.tenLoai,
            pc.tenPhongCach,
            ct.soLuong,
            spct.donGia,
            ct.donGia,
            ct.thanhTien,
            CASE WHEN spct.donGia > ct.donGia THEN true ELSE false END,
            CASE WHEN spct.donGia > ct.donGia THEN spct.donGia - ct.donGia ELSE 0 END,
            null,
            null,
            null,
            ct.soLuongHoanKho
        )
        FROM HoaDonChiTiet ct
        JOIN ct.chiTietSanPham spct
        JOIN spct.sanPham      sp
        JOIN spct.mauSac       ms
        JOIN spct.kichCo       kc
        JOIN spct.kieuDang     kd
        JOIN spct.loaiAo       la
        JOIN spct.phongCachMac pc
        WHERE ct.hoaDon.id = :idHoaDon
        """)
    List<HoaDonChiTietDTO> findChiTietByHoaDonId(@Param("idHoaDon") Integer idHoaDon);

    @Query("""
        SELECT CASE WHEN COUNT(ct) > 0 THEN true ELSE false END
        FROM HoaDonChiTiet ct
        JOIN ct.hoaDon hd
        JOIN ct.chiTietSanPham spct
        WHERE hd.khachHang.id = :customerId
          AND spct.sanPham.idSp = :productId
          AND LOWER(COALESCE(hd.trangThai, '')) IN ('hoàn thành', 'hoan thanh', 'hoàn tất', 'hoan tat')
        """)
    boolean hasCompletedOrderProduct(@Param("customerId") Integer customerId,
                                     @Param("productId") Integer productId);
}
