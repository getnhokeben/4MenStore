package com.example.sp.repository.hoadon;

import com.example.sp.model.hoadon.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {

    @EntityGraph(attributePaths = "phuongThucThanhToan")
    List<ThanhToan> findByHoaDon_Id(Integer idHoaDon);

    Optional<ThanhToan> findFirstByMaGiaoDichOrderByIdDesc(String maGiaoDich);
}
