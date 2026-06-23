package com.example.sp.service.order;

import com.example.sp.model.order.LichSuThanhToan;

import java.util.List;

public interface LichSuThanhToanService {

    List<LichSuThanhToan> getByHoaDon(Integer idHoaDon);

}