package com.example.sp.service.promotion;

import com.example.sp.dto.promotion.PhieuGiamGiaRequest;
import com.example.sp.model.promotion.PhieuGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PhieuGiamGiaService {

    Page<PhieuGiamGia> getAll(String keyword, String loaiGiam, Boolean trangThai, String tienDo, LocalDateTime tuNgay, LocalDateTime denNgay, Pageable pageable);

    PhieuGiamGia findById(Integer id);

    PhieuGiamGia save(PhieuGiamGiaRequest request);

    void delete(Integer id);

    PhieuGiamGia toggleStatus(Integer id);

    List<Integer> getKhachHangIds(Integer idPgg);

    boolean validateVoucher(Integer idVoucher, Double tongTien);
}
