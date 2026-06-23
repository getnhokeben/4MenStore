package com.example.sp.repository;

import com.example.sp.model.employee.NhanVien;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    @Query("""
        SELECT n FROM NhanVien n
        WHERE (COALESCE(:key, '') = '' 
               OR LOWER(n.hoTen) LIKE LOWER(CONCAT('%', :key, '%'))
               OR LOWER(n.soDienThoai) LIKE LOWER(CONCAT('%', :key, '%'))
               OR LOWER(n.email) LIKE LOWER(CONCAT('%', :key, '%'))
               OR LOWER(n.maNv) LIKE LOWER(CONCAT('%', :key, '%'))
               OR LOWER(n.cccd) LIKE LOWER(CONCAT('%', :key, '%')))
          AND (COALESCE(:vaiTro, '') = '' OR n.vaiTro = :vaiTro)
          AND (:trangThai IS NULL OR n.trangThai = :trangThai)
    """)
    Page<NhanVien> search(
            @Param("key") String key,
            @Param("vaiTro") String vaiTro,
            @Param("trangThai") Boolean trangThai,
            Pageable pageable
    );

    /**
     * Tìm mã nhân viên lớn nhất để tạo mã tự động tăng NV001, NV002...
     * Sử dụng SQL Server TOP 1 để lấy 1 bản ghi đầu tiên
     */
    @Query(value = """
        SELECT TOP 1 ma_nv 
        FROM nhan_vien 
        WHERE ma_nv LIKE 'NV%' 
        ORDER BY CAST(SUBSTRING(ma_nv, 3, LEN(ma_nv)) AS INT) DESC
        """, nativeQuery = true)
    String findMaxMaNv();

    boolean existsByMaNv(String maNv);
    boolean existsByEmail(String email);
    boolean existsByCccd(String cccd);
    boolean existsByMaNvAndIdNot(String maNv, Integer id);
    boolean existsByEmailAndIdNot(String email, Integer id);
    boolean existsByCccdAndIdNot(String cccd, Integer id);
    Optional<NhanVien> findByEmail(String email);
}