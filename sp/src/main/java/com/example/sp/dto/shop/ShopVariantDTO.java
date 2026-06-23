package com.example.sp.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopVariantDTO {
    private Integer idSpct;
    private Integer idSanPham;
    private String maChiTietSanPham;
    private String tenSanPham;
    private String hinhAnh;
    private String imageUrl;
    private Integer soLuongTon;
    private BigDecimal donGia;
    private ShopLookupDTO kichCo;
    private ShopLookupDTO mauSac;
    private ShopLookupDTO loaiAo;
    private ShopLookupDTO phongCachMac;
    private ShopLookupDTO kieuDang;
}
