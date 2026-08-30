package com.example.sp.service.hoadon;

import com.example.sp.model.hoadon.LichSuThanhToan;

import java.util.List;

public interface LichSuThanhToanService {

    List<LichSuThanhToan> getByHoaDon(Integer idHoaDon);

}
