
package com.example.sp.repository.khachhang;

import com.example.sp.model.khachhang.KhachHang;
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
        WHERE (
            :keyword IS NULL
            OR LOWER(COALESCE(k.tenKhachHang, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(k.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR COALESCE(k.soDienThoai, '') LIKE CONCAT('%', :keyword, '%')
            OR COALESCE(k.maKh, '') LIKE CONCAT('%', :keyword, '%')
            OR COALESCE(k.cccd, '') LIKE CONCAT('%', :keyword, '%')
        )
        AND (:trangThai IS NULL OR k.trangThai = :trangThai)
        AND (:gioiTinh IS NULL OR k.gioiTinh = :gioiTinh)
    """)
    Page<KhachHang> search(
            @Param("keyword") String keyword,
            @Param("trangThai") Boolean trangThai,
            @Param("gioiTinh") String gioiTinh,
            Pageable pageable
    );

    Optional<KhachHang> findByCccd(String cccd);

    Optional<KhachHang> findBySoDienThoai(String soDienThoai);

    boolean existsByMaKh(String maKh);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Integer id
    );

    boolean existsBySoDienThoai(String soDienThoai);

    boolean existsBySoDienThoaiAndIdNot(
            String soDienThoai,
            Integer id
    );

    boolean existsByCccd(String cccd);

    boolean existsByCccdAndIdNot(
            String cccd,
            Integer id
    );
    Optional<KhachHang> findByEmailIgnoreCase(String email);
}
