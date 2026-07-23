package com.example.sp.service.nhanvien;

import com.example.sp.model.nhanvien.NhanVien;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NhanVienService {

    Page<NhanVien> getAll(
            String keyword,
            String vaiTro,
            Boolean trangThai,
            Pageable pageable
    );

    NhanVien findById(Integer id);

    NhanVien create(NhanVien nhanVien);

    NhanVien update(Integer id, NhanVien nhanVien);

    NhanVien toggleStatus(Integer id);

    void deactivate(Integer id);
}