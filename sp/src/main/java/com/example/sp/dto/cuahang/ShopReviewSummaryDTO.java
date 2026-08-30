package com.example.sp.dto.cuahang;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ShopReviewSummaryDTO {
    private BigDecimal diemTrungBinh;
    private long tongDanhGia;
    private Map<Integer, Long> thongKeSao;
    private List<ShopReviewDTO> danhSach;
}
