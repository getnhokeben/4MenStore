package com.example.sp.service.order;

import com.example.sp.model.order.ThanhToan;

public interface ThanhToanService {

    ThanhToan thanhToanTienMat(Integer idHoaDon);

    ThanhToan thanhToanOnline(Integer idHoaDon, Integer idPttt);
}