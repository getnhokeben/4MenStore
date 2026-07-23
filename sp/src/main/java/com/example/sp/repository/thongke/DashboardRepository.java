package com.example.sp.repository.thongke;

import com.example.sp.model.hoadon.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<HoaDon, Integer> {

    @Query(value = """
            SELECT COUNT(*)
            FROM hoa_don
            WHERE (:from IS NULL OR ngay_tao >= :from)
              AND (:to IS NULL OR ngay_tao < DATEADD(day, 1, :to))
            """, nativeQuery = true)
    int totalOrder(@Param("from") String from, @Param("to") String to);

    @Query(value = """
            SELECT SUM(tong_tien_thanh_toan)
            FROM hoa_don
            WHERE (ngay_thanh_toan IS NOT NULL
                   OR trang_thai IN (N'\u0110\u00e3 thanh to\u00e1n', N'Ho\u00e0n th\u00e0nh'))
              AND (:from IS NULL OR ngay_tao >= :from)
              AND (:to IS NULL OR ngay_tao < DATEADD(day, 1, :to))
            """, nativeQuery = true)
    Double totalRevenue(@Param("from") String from, @Param("to") String to);

    @Query(value = """
            SELECT SUM(tt.so_tien)
            FROM thanh_toan tt
            JOIN phuong_thuc_thanh_toan pttt ON tt.id_pttt = pttt.id_pttt
            JOIN hoa_don hd ON tt.id_hoa_don = hd.id_hoa_don
            WHERE ((:method = 'CASH'
                    AND (UPPER(pttt.ma_pttt) IN ('TIEN_MAT', 'TM', 'CASH')
                         OR UPPER(pttt.ten_pttt) LIKE N'%TI\u1ec0N M\u1eb6T%'
                         OR UPPER(pttt.ten_pttt) LIKE '%CASH%'))
                OR (:method = 'TRANSFER'
                    AND (UPPER(pttt.ma_pttt) IN ('CHUYEN_KHOAN', 'CK', 'TRANSFER', 'BANK_TRANSFER')
                         OR UPPER(pttt.ten_pttt) LIKE N'%CHUY\u1ec2N KHO\u1ea2N%'
                         OR UPPER(pttt.ten_pttt) LIKE '%TRANSFER%')))
              AND (tt.trang_thai IS NULL OR tt.trang_thai IN (N'Th\u00e0nh c\u00f4ng', N'Thanh cong'))
              AND (:from IS NULL OR hd.ngay_tao >= :from)
              AND (:to IS NULL OR hd.ngay_tao < DATEADD(day, 1, :to))
            """, nativeQuery = true)
    Double paymentRevenue(@Param("from") String from, @Param("to") String to, @Param("method") String method);

    @Query(value = """
            SELECT CAST(ngay_tao AS DATE), SUM(tong_tien_thanh_toan)
            FROM hoa_don
            WHERE (ngay_thanh_toan IS NOT NULL
                   OR trang_thai IN (N'\u0110\u00e3 thanh to\u00e1n', N'Ho\u00e0n th\u00e0nh'))
              AND (:from IS NULL OR ngay_tao >= :from)
              AND (:to IS NULL OR ngay_tao < DATEADD(day, 1, :to))
            GROUP BY CAST(ngay_tao AS DATE)
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> chartData(@Param("from") String from, @Param("to") String to);

    @Query(value = """
            SELECT CASE
                       WHEN trang_thai = N'Đã thanh toán' THEN N'Hoàn thành'
                       ELSE trang_thai
                   END AS trang_thai_hien_thi,
                   COUNT(*)
            FROM hoa_don
            WHERE trang_thai IN (
                      N'Chờ xác nhận',
                      N'Đã xác nhận',
                      N'Đang chuẩn bị hàng',
                      N'Đang giao hàng',
                      N'Chờ hàng hoàn',
                      N'Hoàn thành',
                      N'Đã thanh toán',
                      N'Đã hủy'
                  )
              AND (:from IS NULL OR ngay_tao >= :from)
              AND (:to IS NULL OR ngay_tao < DATEADD(day, 1, :to))
            GROUP BY CASE
                         WHEN trang_thai = N'Đã thanh toán' THEN N'Hoàn thành'
                         ELSE trang_thai
                     END
            ORDER BY CASE CASE
                              WHEN trang_thai = N'Đã thanh toán' THEN N'Hoàn thành'
                              ELSE trang_thai
                          END
                         WHEN N'Chờ xác nhận' THEN 1
                         WHEN N'Đã xác nhận' THEN 2
                         WHEN N'Đang chuẩn bị hàng' THEN 3
                         WHEN N'Đang giao hàng' THEN 4
                         WHEN N'Chờ hàng hoàn' THEN 5
                         WHEN N'Hoàn thành' THEN 6
                         WHEN N'Đã hủy' THEN 7
                         ELSE 8
                     END
            """, nativeQuery = true)
    List<Object[]> status(@Param("from") String from, @Param("to") String to);

    @Query(value = """
            SELECT COALESCE(loai_don, N'Kh\u00e1c'), COUNT(*)
            FROM hoa_don
            WHERE (:from IS NULL OR ngay_tao >= :from)
              AND (:to IS NULL OR ngay_tao < DATEADD(day, 1, :to))
            GROUP BY loai_don
            """, nativeQuery = true)
    List<Object[]> channel(@Param("from") String from, @Param("to") String to);

    @Query(value = """
            SELECT TOP 10 sp.ten_sp,
                   ct.so_luong_ton,
                   ct.don_gia,
                   COALESCE(NULLIF(ct.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(sp.hinh_anh, '')) AS hinh_anh
            FROM chi_tiet_san_pham ct
            JOIN san_pham sp ON ct.id_san_pham = sp.id_sp
            OUTER APPLY (
                SELECT TOP 1 url_anh
                FROM hinh_anh_san_pham
                WHERE id_san_pham = sp.id_sp
                  AND NULLIF(url_anh, '') IS NOT NULL
                ORDER BY id_hinh_anh
            ) img
            WHERE COALESCE(ct.so_luong_ton, 0) <= 10
              AND (ct.trang_thai IS NULL OR ct.trang_thai = 1)
            ORDER BY COALESCE(ct.so_luong_ton, 0), sp.ten_sp
            """, nativeQuery = true)
    List<Object[]> lowStock();

    @Query(value = """
            SELECT TOP 5 sp.ten_sp,
                   SUM(hdct.so_luong),
                   MAX(ct.don_gia),
                   MAX(COALESCE(NULLIF(ct.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(sp.hinh_anh, ''))) AS hinh_anh
            FROM hoa_don_chi_tiet hdct
            JOIN chi_tiet_san_pham ct ON hdct.id_spct = ct.id_spct
            JOIN san_pham sp ON ct.id_san_pham = sp.id_sp
            JOIN hoa_don hd ON hd.id_hoa_don = hdct.id_hoa_don
            OUTER APPLY (
                SELECT TOP 1 url_anh
                FROM hinh_anh_san_pham
                WHERE id_san_pham = sp.id_sp
                  AND NULLIF(url_anh, '') IS NOT NULL
                ORDER BY id_hinh_anh
            ) img
            WHERE (hd.ngay_thanh_toan IS NOT NULL
                   OR hd.trang_thai IN (N'\u0110\u00e3 thanh to\u00e1n', N'Ho\u00e0n th\u00e0nh'))
            GROUP BY sp.ten_sp
            ORDER BY SUM(hdct.so_luong) DESC
            """, nativeQuery = true)
    List<Object[]> topProduct();

    @Query(value = """
            SELECT TOP 5 sp.ten_sp,
                   SUM(hdct.so_luong),
                   MAX(ct.don_gia),
                   MAX(COALESCE(NULLIF(ct.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(sp.hinh_anh, ''))) AS hinh_anh
            FROM hoa_don_chi_tiet hdct
            JOIN chi_tiet_san_pham ct ON hdct.id_spct = ct.id_spct
            JOIN san_pham sp ON ct.id_san_pham = sp.id_sp
            JOIN hoa_don hd ON hd.id_hoa_don = hdct.id_hoa_don
            OUTER APPLY (
                SELECT TOP 1 url_anh
                FROM hinh_anh_san_pham
                WHERE id_san_pham = sp.id_sp
                  AND NULLIF(url_anh, '') IS NOT NULL
                ORDER BY id_hinh_anh
            ) img
            WHERE (hd.ngay_thanh_toan IS NOT NULL
                   OR hd.trang_thai IN (N'\u0110\u00e3 thanh to\u00e1n', N'Ho\u00e0n th\u00e0nh'))
              AND (:from IS NULL OR hd.ngay_tao >= :from)
              AND (:to IS NULL OR hd.ngay_tao < DATEADD(day, 1, :to))
            GROUP BY sp.ten_sp
            ORDER BY SUM(hdct.so_luong) DESC
            """, nativeQuery = true)
    List<Object[]> topProduct(@Param("from") String from, @Param("to") String to);

    @Query(value = """
            SELECT TOP 5 sp.ten_sp,
                   SUM(hdct.so_luong),
                   MAX(ct.don_gia),
                   MAX(COALESCE(NULLIF(ct.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(sp.hinh_anh, ''))) AS hinh_anh
            FROM hoa_don_chi_tiet hdct
            JOIN chi_tiet_san_pham ct ON hdct.id_spct = ct.id_spct
            JOIN san_pham sp ON ct.id_san_pham = sp.id_sp
            JOIN hoa_don hd ON hd.id_hoa_don = hdct.id_hoa_don
            OUTER APPLY (
                SELECT TOP 1 url_anh
                FROM hinh_anh_san_pham
                WHERE id_san_pham = sp.id_sp
                  AND NULLIF(url_anh, '') IS NOT NULL
                ORDER BY id_hinh_anh
            ) img
            WHERE (hd.ngay_thanh_toan IS NOT NULL
                   OR hd.trang_thai IN (N'\u0110\u00e3 thanh to\u00e1n', N'Ho\u00e0n th\u00e0nh'))
              AND CAST(hd.ngay_tao AS DATE) = CAST(GETDATE() AS DATE)
            GROUP BY sp.ten_sp
            ORDER BY SUM(hdct.so_luong) DESC
            """, nativeQuery = true)
    List<Object[]> topProductToday();

    @Query(value = """
            SELECT TOP 5 sp.ten_sp,
                   SUM(hdct.so_luong),
                   MAX(ct.don_gia),
                   MAX(COALESCE(NULLIF(ct.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(sp.hinh_anh, ''))) AS hinh_anh
            FROM hoa_don_chi_tiet hdct
            JOIN chi_tiet_san_pham ct ON hdct.id_spct = ct.id_spct
            JOIN san_pham sp ON ct.id_san_pham = sp.id_sp
            JOIN hoa_don hd ON hd.id_hoa_don = hdct.id_hoa_don
            OUTER APPLY (
                SELECT TOP 1 url_anh
                FROM hinh_anh_san_pham
                WHERE id_san_pham = sp.id_sp
                  AND NULLIF(url_anh, '') IS NOT NULL
                ORDER BY id_hinh_anh
            ) img
            WHERE (hd.ngay_thanh_toan IS NOT NULL
                   OR hd.trang_thai IN (N'\u0110\u00e3 thanh to\u00e1n', N'Ho\u00e0n th\u00e0nh'))
              AND hd.ngay_tao >= DATEADD(day, -7, GETDATE())
            GROUP BY sp.ten_sp
            ORDER BY SUM(hdct.so_luong) DESC
            """, nativeQuery = true)
    List<Object[]> topProductWeek();
}
