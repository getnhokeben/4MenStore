package com.example.sp.service.employee;

import com.example.sp.model.employee.NhanVien;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NhanVienService {

    Page<NhanVien> getAll(String keyword, String vaiTro, Boolean trangThai, Pageable pageable);

    NhanVien findById(Integer id);

    NhanVien save(NhanVien nv);

    void delete(Integer id);
}