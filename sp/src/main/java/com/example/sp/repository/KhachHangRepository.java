package com.example.sp.repository;

import com.example.sp.model.customer.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

    @Query("""
        SELECT k FROM KhachHang k
        WHERE (:key IS NULL
               OR k.tenKhachHang LIKE CONCAT('%', :key, '%')
               OR k.soDienThoai LIKE CONCAT('%', :key, '%')
               OR k.email LIKE CONCAT('%', :key, '%')
               OR k.maKh LIKE CONCAT('%', :key, '%')
               OR k.cccd LIKE CONCAT('%', :key, '%'))
          AND (:trangThai IS NULL OR k.trangThai = :trangThai)
    """)
    Page<KhachHang> search(
            @Param("key") String key,
            @Param("trangThai") Boolean trangThai,
            Pageable pageable
    );

    Optional<KhachHang> findBySoDienThoai(String soDienThoai);

    @Query("""
        SELECT k FROM KhachHang k
        WHERE LOWER(k.email) = LOWER(:identifier)
           OR k.soDienThoai = :identifier
           OR LOWER(k.tenTaiKhoan) = LOWER(:identifier)
    """)
    Optional<KhachHang> findByLoginIdentifier(@Param("identifier") String identifier);

    boolean existsByMaKh(String maKh);
    boolean existsByEmail(String email);
    boolean existsBySoDienThoai(String soDienThoai);
    boolean existsByTenTaiKhoanIgnoreCase(String tenTaiKhoan);
    boolean existsByCccd(String cccd);
    boolean existsByMaKhAndIdNot(String maKh, Integer id);
    boolean existsByEmailAndIdNot(String email, Integer id);
    boolean existsBySoDienThoaiAndIdNot(String soDienThoai, Integer id);
    boolean existsByTenTaiKhoanIgnoreCaseAndIdNot(String tenTaiKhoan, Integer id);
    boolean existsByCccdAndIdNot(String cccd, Integer id);

    @Query(value = """
        SELECT TOP 1 ma_kh 
        FROM khach_hang 
        WHERE ma_kh LIKE 'KH%' 
        ORDER BY CAST(SUBSTRING(ma_kh, 3, LEN(ma_kh)) AS INT) DESC
        """, nativeQuery = true)
    String findMaxMaKh();
}