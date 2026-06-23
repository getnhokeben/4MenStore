package com.example.sp.service.order;

import com.example.sp.dto.HoaDonChiTietDTO;

import java.util.List;

public interface HoaDonChiTietService {

    void themSanPham(Integer idHoaDon, Integer idSpct, Integer soLuong);

    void capNhatSoLuong(Integer idHdct, Integer soLuong);

    void xoaSanPham(Integer idHdct);

    List<HoaDonChiTietDTO> getByHoaDonId(Integer idHoaDon);
}