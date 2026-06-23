package com.example.sp.repository;

import com.example.sp.model.order.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<HoaDon, Integer> {

    @Query(value = "SELECT COUNT(*) FROM hoa_don WHERE (:from IS NULL OR ngay_tao >= :from) AND (:to IS NULL OR ngay_tao <= :to)", nativeQuery = true)
    int totalOrder(@Param("from") String from, @Param("to") String to);

    @Query(value = "SELECT SUM(tong_tien_thanh_toan) FROM hoa_don WHERE (:from IS NULL OR ngay_tao >= :from) AND (:to IS NULL OR ngay_tao <= :to)", nativeQuery = true)
    Double totalRevenue(@Param("from") String from, @Param("to") String to);

    @Query(value = "SELECT SUM(tong_tien_thanh_toan) FROM hoa_don WHERE trang_thai = N'Đã thanh toán' AND (:from IS NULL OR ngay_tao >= :from) AND (:to IS NULL OR ngay_tao <= :to)", nativeQuery = true)
    Double realRevenue(@Param("from") String from, @Param("to") String to);

    @Query(value = "SELECT CAST(ngay_tao AS DATE), SUM(tong_tien_thanh_toan) FROM hoa_don WHERE trang_thai = N'Đã thanh toán' GROUP BY CAST(ngay_tao AS DATE) ORDER BY 1", nativeQuery = true)
    List<Object[]> chartData();

    @Query(value = "SELECT trang_thai, COUNT(*) FROM hoa_don GROUP BY trang_thai", nativeQuery = true)
    List<Object[]> status();

    @Query(value = "SELECT loai_don, COUNT(*) FROM hoa_don GROUP BY loai_don", nativeQuery = true)
    List<Object[]> channel();

    @Query(value = "SELECT TOP 10 sp.ten_sp, ct.so_luong_ton, ct.gia_ban, sp.ma_sp FROM chi_tiet_san_pham ct JOIN san_pham sp ON ct.id_san_pham = sp.id_sp WHERE ct.so_luong_ton <= 10", nativeQuery = true)
    List<Object[]> lowStock();

    @Query(value = "SELECT TOP 5 sp.ten_sp, SUM(hdct.so_luong), MAX(ct.gia_ban), sp.hinh_anh FROM hoa_don_chi_tiet hdct JOIN chi_tiet_san_pham ct ON hdct.id_spct = ct.id_spct JOIN san_pham sp ON ct.id_san_pham = sp.id_sp JOIN hoa_don hd ON hd.id_hoa_don = hdct.id_hoa_don WHERE hd.trang_thai = N'Đã thanh toán' GROUP BY sp.ten_sp, sp.hinh_anh ORDER BY SUM(hdct.so_luong) DESC", nativeQuery = true)
    List<Object[]> topProduct();

    @Query(value = "SELECT TOP 5 sp.ten_sp, SUM(hdct.so_luong), MAX(ct.gia_ban), sp.hinh_anh FROM hoa_don_chi_tiet hdct JOIN chi_tiet_san_pham ct ON hdct.id_spct = ct.id_spct JOIN san_pham sp ON ct.id_san_pham = sp.id_sp JOIN hoa_don hd ON hd.id_hoa_don = hdct.id_hoa_don WHERE hd.trang_thai = N'Đã thanh toán' AND CAST(hd.ngay_tao AS DATE) = CAST(GETDATE() AS DATE) GROUP BY sp.ten_sp, sp.hinh_anh ORDER BY SUM(hdct.so_luong) DESC", nativeQuery = true)
    List<Object[]> topProductToday();

    @Query(value = "SELECT TOP 5 sp.ten_sp, SUM(hdct.so_luong), MAX(ct.gia_ban), sp.hinh_anh FROM hoa_don_chi_tiet hdct JOIN chi_tiet_san_pham ct ON hdct.id_spct = ct.id_spct JOIN san_pham sp ON ct.id_san_pham = sp.id_sp JOIN hoa_don hd ON hd.id_hoa_don = hdct.id_hoa_don WHERE hd.trang_thai = N'Đã thanh toán' AND hd.ngay_tao >= DATEADD(day, -7, GETDATE()) GROUP BY sp.ten_sp, sp.hinh_anh ORDER BY SUM(hdct.so_luong) DESC", nativeQuery = true)
    List<Object[]> topProductWeek();
}
