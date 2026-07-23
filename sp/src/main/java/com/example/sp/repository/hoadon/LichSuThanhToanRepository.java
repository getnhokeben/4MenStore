package com.example.sp.repository.hoadon;

import com.example.sp.model.hoadon.LichSuThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuThanhToanRepository extends JpaRepository<LichSuThanhToan, Integer> {

    // Lấy lịch sử thanh toán của 1 hóa đơn, mới nhất trước
    List<LichSuThanhToan> findByHoaDon_IdOrderByNgayThanhToanDesc(Integer idHoaDon);

    boolean existsByMaGiaoDich(String maGiaoDich);
}
