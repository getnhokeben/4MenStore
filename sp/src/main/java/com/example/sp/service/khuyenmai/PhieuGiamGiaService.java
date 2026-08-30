package com.example.sp.service.khuyenmai;

import com.example.sp.dto.khuyenmai.PhieuGiamGiaRequest;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface PhieuGiamGiaService {

    Page<PhieuGiamGia> getAll(String keyword, String loaiGiam, Boolean trangThai, String tienDo, LocalDateTime tuNgay, LocalDateTime denNgay, Pageable pageable);

    PhieuGiamGia findById(Integer id);

    PhieuGiamGia save(PhieuGiamGiaRequest request);

    void delete(Integer id);

    PhieuGiamGia toggleStatus(Integer id);

    boolean validateVoucher(Integer idVoucher, Double tongTien);
}
