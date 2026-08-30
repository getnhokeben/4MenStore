package com.example.sp.dto.cuahang;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ShopOrderHistoryItemDTO {
    private Integer id;
    private Integer idSpct;
    private Integer idSp;
    private String maSanPham;
    private String tenSanPham;
    private String mauSac;
    private String kichCo;
    private Integer soLuong;
    private BigDecimal giaGoc;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private Boolean dangGiamGia;
    private BigDecimal soTienGiam;
    private String hinhAnh;
    private String imageUrl;
}
