package com.example.sp.repository;

import com.example.sp.dto.HoaDonChiTietDTO;
import com.example.sp.model.order.HoaDonChiTiet;
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
        SELECT new com.example.sp.dto.HoaDonChiTietDTO(
            ct.id,
            sp.maSp,
            sp.tenSp,
            ms.tenMauSac,
            kc.tenKichCo,
            kd.tenKieuDang,
            la.tenLoai,
            pc.tenPhongCach,
            ct.soLuong,
            ct.donGia,
            ct.thanhTien
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
}