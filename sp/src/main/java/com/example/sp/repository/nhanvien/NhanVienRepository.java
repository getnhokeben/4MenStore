
package com.example.sp.repository.nhanvien;

import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.service.nhanvien.EmployeeAccountMailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    @Query("""
        SELECT n FROM NhanVien n
        WHERE (
            :keyword IS NULL
            OR LOWER(COALESCE(n.hoTen, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(n.soDienThoai, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(n.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(n.maNv, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(n.cccd, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(n.diaChi, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(n.diaChiChiTiet, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(n.phuongXa, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(n.quanHuyen, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(n.tinhThanh, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:vaiTro IS NULL OR n.vaiTro = :vaiTro)
        AND (:trangThai IS NULL OR n.trangThai = :trangThai)
    """)
    Page<NhanVien> search(
            @Param("keyword") String keyword,
            @Param("vaiTro") String vaiTro,
            @Param("trangThai") Boolean trangThai,
            Pageable pageable
    );

    boolean existsByMaNv(String maNv);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Integer id
    );

    boolean existsByCccd(String cccd);

    boolean existsByCccdAndIdNot(
            String cccd,
            Integer id
    );

    boolean existsBySoDienThoai(String soDienThoai);

    boolean existsBySoDienThoaiAndIdNot(
            String soDienThoai,
            Integer id
    );

    boolean existsByVaiTroAndTrangThaiTrueAndIdNot(
            String vaiTro,
            Integer id
    );

    Optional<NhanVien> findByEmailIgnoreCase(String email);

    Optional<NhanVien> findByCccd(String cccd);
}
