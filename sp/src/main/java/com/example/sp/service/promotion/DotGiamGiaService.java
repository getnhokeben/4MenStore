package com.example.sp.service.promotion;


import com.example.sp.dto.promotion.DotGiamGiaRequest;
import com.example.sp.dto.promotion.SanPhamChiTietPromotionView;
import com.example.sp.model.promotion.DotGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface DotGiamGiaService {

    List<DotGiamGia> getAll();

    Page<DotGiamGia> getAll(String keyword, Boolean trangThai, String tienDo, LocalDateTime tuNgay, LocalDateTime denNgay, Pageable pageable);

    DotGiamGia findById(Integer id);

    DotGiamGia save(DotGiamGia dg);

    DotGiamGia save(DotGiamGiaRequest request);

    void delete(Integer id);

    DotGiamGia toggleStatus(Integer id);

    List<Integer> getSelectedSpctIds(Integer idDotGiamGia);

    List<SanPhamChiTietPromotionView> getSanPhamChiTietKichHoat();
}
