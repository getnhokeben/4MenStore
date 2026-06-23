package com.example.sp.service.customer;

import com.example.sp.model.customer.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KhachHangService {

    Page<KhachHang> getAll(String keyword, Boolean trangThai, Pageable pageable);

    KhachHang findById(Integer id);

    KhachHang save(KhachHang kh);

    void delete(Integer id);
}