package com.example.sp.repository.khachhang;

import com.example.sp.model.khachhang.DiaChi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaChiRepository extends JpaRepository<DiaChi, Integer> {

    List<DiaChi> findByKhachHang_IdOrderByMacDinhDescIdAsc(Integer customerId);

    Optional<DiaChi> findByIdAndKhachHang_Id(
            Integer addressId,
            Integer customerId
    );

    Optional<DiaChi> findFirstByKhachHang_IdAndMacDinhTrueOrderByIdAsc(
            Integer customerId
    );

    long countByKhachHang_Id(Integer customerId);

    @Modifying
    @Query("""
        UPDATE DiaChi d
        SET d.macDinh = false
        WHERE d.khachHang.id = :customerId
          AND d.macDinh = true
    """)
    int clearDefaultByCustomerId(@Param("customerId") Integer customerId);

    @Modifying
    @Query("DELETE FROM DiaChi d WHERE d.khachHang.id = :customerId")
    int deleteByKhachHang_Id(@Param("customerId") Integer customerId);
}
