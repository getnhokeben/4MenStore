package com.example.sp.dto.pos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosOrderItemDTO {
    private Integer idHdct;
    private Integer idSpct;
    private String maSp;
    private String tenSanPham;
    private String maChiTietSanPham;
    private String hinhAnh;
    private String imageUrl;
    private String mauSac;
    private String kichCo;
    private BigDecimal donGia;
    private Integer soLuong;
    private Integer soLuongTon;
    private BigDecimal thanhTien;
}
