package com.example.sp.repository.hoadon;

import com.example.sp.model.hoadon.PhuongThucThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhuongThucThanhToanRepository extends JpaRepository<PhuongThucThanhToan, Integer> {
    Optional<PhuongThucThanhToan> findByTenPttt(String tenPttt);

    Optional<PhuongThucThanhToan> findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase(String maPttt, String tenPttt);
}
