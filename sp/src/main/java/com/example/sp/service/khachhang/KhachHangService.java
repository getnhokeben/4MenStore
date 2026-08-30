package com.example.sp.service.khachhang;

import com.example.sp.model.khachhang.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KhachHangService {

    Page<KhachHang> getAll(
            String keyword,
            Boolean trangThai,
            String gioiTinh,
            Pageable pageable
    );

    KhachHang findById(Integer id);

    KhachHang create(KhachHang customer);

    KhachHang update(Integer id, KhachHang customer);

    KhachHang toggleStatus(Integer id);

    void deactivate(Integer id);
}
