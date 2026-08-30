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
              AND (:trangThai IS NULL OR trang_thai = :trangThai)
            """, nativeQuery = true)
    int totalOrder(@Param("from") String from, @Param("to") String to, @Param("trangThai") String trangThai);

    @Query(value = """
            SELECT SUM(CASE
                         WHEN COALESCE(tong_tien_thanh_toan, 0) <= COALESCE(phi_van_chuyen, 0) THEN 0
                         ELSE COALESCE(tong_tien_thanh_toan, 0) - COALESCE(phi_van_chuyen, 0)
                       END)
            FROM hoa_don
            WHERE (ngay_thanh_toan IS NOT NULL
                   OR trang_thai IN (N'\u0110\u00e3 thanh to\u00e1n', N'Ho\u00e0n th\u00e0nh'))
              AND COALESCE(trang_thai, N'') NOT IN (N'Đã hủy', N'Hủy', N'Hủy đơn', N'Chờ hàng hoàn', N'Chờ hoàn hàng')
              AND (:from IS NULL OR ngay_tao >= :from)
              AND (:to IS NULL OR ngay_tao < DATEADD(day, 1, :to))
              AND (:trangThai IS NULL OR trang_thai = :trangThai)
            """, nativeQuery = true)
    Double totalRevenue(@Param("from") String from, @Param("to") String to, @Param("trangThai") String trangThai);

    @Query(value = """
            SELECT SUM(CASE
                         WHEN COALESCE(tt.so_tien, 0) <= COALESCE(hd.phi_van_chuyen, 0) THEN 0
                         ELSE COALESCE(tt.so_tien, 0) - COALESCE(hd.phi_van_chuyen, 0)
                       END)
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
              AND COALESCE(hd.trang_thai, N'') NOT IN (N'Đã hủy', N'Hủy', N'Hủy đơn', N'Chờ hàng hoàn', N'Chờ hoàn hàng')
              AND (:from IS NULL OR hd.ngay_tao >= :from)
              AND (:to IS NULL OR hd.ngay_tao < DATEADD(day, 1, :to))
              AND (:trangThai IS NULL OR hd.trang_thai = :trangThai)
            """, nativeQuery = true)
    Double paymentRevenue(
            @Param("from") String from,
            @Param("to") String to,
            @Param("method") String method,
            @Param("trangThai") String trangThai
    );

    @Query(value = """
            SELECT CAST(ngay_tao AS DATE),
                   SUM(CASE
                         WHEN COALESCE(tong_tien_thanh_toan, 0) <= COALESCE(phi_van_chuyen, 0) THEN 0
                         ELSE COALESCE(tong_tien_thanh_toan, 0) - COALESCE(phi_van_chuyen, 0)
                       END)
            FROM hoa_don
            WHERE (ngay_thanh_toan IS NOT NULL
                   OR trang_thai IN (N'\u0110\u00e3 thanh to\u00e1n', N'Ho\u00e0n th\u00e0nh'))
              AND COALESCE(trang_thai, N'') NOT IN (N'Đã hủy', N'Hủy', N'Hủy đơn', N'Chờ hàng hoàn', N'Chờ hoàn hàng')
              AND (:from IS NULL OR ngay_tao >= :from)
              AND (:to IS NULL OR ngay_tao < DATEADD(day, 1, :to))
              AND (:trangThai IS NULL OR trang_thai = :trangThai)
            GROUP BY CAST(ngay_tao AS DATE)
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> chartData(@Param("from") String from, @Param("to") String to, @Param("trangThai") String trangThai);

    @Query(value = """
            SELECT COALESCE(NULLIF(LTRIM(RTRIM(trang_thai)), N''), N'Không xác định') AS trang_thai_hien_thi,
                   COUNT(*)
            FROM hoa_don
            WHERE (:from IS NULL OR ngay_tao >= :from)
              AND (:to IS NULL OR ngay_tao < DATEADD(day, 1, :to))
              AND (:trangThai IS NULL OR trang_thai = :trangThai)
            GROUP BY COALESCE(NULLIF(LTRIM(RTRIM(trang_thai)), N''), N'Không xác định')
            ORDER BY CASE COALESCE(NULLIF(LTRIM(RTRIM(trang_thai)), N''), N'Không xác định')
                         WHEN N'Chờ thanh toán online' THEN 1
                          WHEN N'Chờ xác nhận' THEN 2
                         WHEN N'Chờ nhập hàng' THEN 3
                         WHEN N'Đã xác nhận' THEN 4
                         WHEN N'Đang chuẩn bị hàng' THEN 5
                         WHEN N'Chờ giao hàng' THEN 6
                         WHEN N'Đang giao hàng' THEN 7
                         WHEN N'Chờ hàng hoàn' THEN 8
                         WHEN N'Hoàn thành' THEN 9
                         WHEN N'Đã thanh toán' THEN 10
                         WHEN N'Đã hủy' THEN 11
                         ELSE 12
                     END
            """, nativeQuery = true)
    List<Object[]> status(@Param("from") String from, @Param("to") String to, @Param("trangThai") String trangThai);

    @Query(value = """
            SELECT COALESCE(loai_don, N'Kh\u00e1c'), COUNT(*)
            FROM hoa_don
            WHERE (:from IS NULL OR ngay_tao >= :from)
              AND (:to IS NULL OR ngay_tao < DATEADD(day, 1, :to))
              AND (:trangThai IS NULL OR trang_thai = :trangThai)
            GROUP BY loai_don
            """, nativeQuery = true)
    List<Object[]> channel(@Param("from") String from, @Param("to") String to, @Param("trangThai") String trangThai);

    @Query(value = """
            SELECT TOP 10 sp.ten_sp,
                   ct.so_luong_ton,
                   ct.don_gia,
                   COALESCE(NULLIF(sp.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(ct.hinh_anh, '')) AS hinh_anh
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
                   MAX(COALESCE(NULLIF(sp.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(ct.hinh_anh, ''))) AS hinh_anh
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
              AND COALESCE(hd.trang_thai, N'') NOT IN (N'Đã hủy', N'Hủy', N'Hủy đơn', N'Chờ hàng hoàn', N'Chờ hoàn hàng')
            GROUP BY sp.ten_sp
            ORDER BY SUM(hdct.so_luong) DESC
            """, nativeQuery = true)
    List<Object[]> topProduct();

    @Query(value = """
            SELECT TOP 5 sp.ten_sp,
                   SUM(hdct.so_luong),
                   MAX(ct.don_gia),
                   MAX(COALESCE(NULLIF(sp.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(ct.hinh_anh, ''))) AS hinh_anh
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
              AND COALESCE(hd.trang_thai, N'') NOT IN (N'Đã hủy', N'Hủy', N'Hủy đơn', N'Chờ hàng hoàn', N'Chờ hoàn hàng')
              AND (:from IS NULL OR hd.ngay_tao >= :from)
              AND (:to IS NULL OR hd.ngay_tao < DATEADD(day, 1, :to))
              AND (:trangThai IS NULL OR hd.trang_thai = :trangThai)
            GROUP BY sp.ten_sp
            ORDER BY SUM(hdct.so_luong) DESC
            """, nativeQuery = true)
    List<Object[]> topProduct(@Param("from") String from, @Param("to") String to, @Param("trangThai") String trangThai);

    @Query(value = """
            SELECT TOP 5 kh.ten_khach_hang,
                   kh.so_dien_thoai,
                   COUNT(DISTINCT hd.id_hoa_don) AS so_don_hang,
                   SUM(CASE
                         WHEN COALESCE(hd.tong_tien_thanh_toan, 0) <= COALESCE(hd.phi_van_chuyen, 0) THEN 0
                         ELSE COALESCE(hd.tong_tien_thanh_toan, 0) - COALESCE(hd.phi_van_chuyen, 0)
                       END) AS tong_chi_tieu
            FROM hoa_don hd
            JOIN khach_hang kh ON hd.id_khach_hang = kh.id_kh
            WHERE (hd.ngay_thanh_toan IS NOT NULL
                   OR hd.trang_thai IN (N'Đã thanh toán', N'Hoàn thành'))
              AND COALESCE(hd.trang_thai, N'') NOT IN (N'Đã hủy', N'Hủy', N'Hủy đơn', N'Chờ hàng hoàn', N'Chờ hoàn hàng')
              AND (:from IS NULL OR hd.ngay_tao >= :from)
              AND (:to IS NULL OR hd.ngay_tao < DATEADD(day, 1, :to))
              AND (:trangThai IS NULL OR hd.trang_thai = :trangThai)
            GROUP BY kh.id_kh, kh.ten_khach_hang, kh.so_dien_thoai
            ORDER BY tong_chi_tieu DESC, so_don_hang DESC, kh.ten_khach_hang
            """, nativeQuery = true)
    List<Object[]> topCustomer(@Param("from") String from, @Param("to") String to, @Param("trangThai") String trangThai);

    @Query(value = """
            SELECT TOP 5 sp.ten_sp,
                   SUM(hdct.so_luong),
                   MAX(ct.don_gia),
                   MAX(COALESCE(NULLIF(sp.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(ct.hinh_anh, ''))) AS hinh_anh
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
              AND COALESCE(hd.trang_thai, N'') NOT IN (N'Đã hủy', N'Hủy', N'Hủy đơn', N'Chờ hàng hoàn', N'Chờ hoàn hàng')
              AND CAST(hd.ngay_tao AS DATE) = CAST(GETDATE() AS DATE)
            GROUP BY sp.ten_sp
            ORDER BY SUM(hdct.so_luong) DESC
            """, nativeQuery = true)
    List<Object[]> topProductToday();

    @Query(value = """
            SELECT TOP 5 sp.ten_sp,
                   SUM(hdct.so_luong),
                   MAX(ct.don_gia),
                   MAX(COALESCE(NULLIF(sp.hinh_anh, ''), NULLIF(img.url_anh, ''), NULLIF(ct.hinh_anh, ''))) AS hinh_anh
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
              AND COALESCE(hd.trang_thai, N'') NOT IN (N'Đã hủy', N'Hủy', N'Hủy đơn', N'Chờ hàng hoàn', N'Chờ hoàn hàng')
              AND hd.ngay_tao >= DATEADD(day, -7, GETDATE())
            GROUP BY sp.ten_sp
            ORDER BY SUM(hdct.so_luong) DESC
            """, nativeQuery = true)
    List<Object[]> topProductWeek();
}
