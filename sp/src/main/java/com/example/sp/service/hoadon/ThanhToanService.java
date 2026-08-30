package com.example.sp.service.hoadon;

import com.example.sp.model.hoadon.ThanhToan;

public interface ThanhToanService {

    ThanhToan thanhToanTienMat(Integer idHoaDon);

    ThanhToan thanhToanOnline(Integer idHoaDon, Integer idPttt);
}
