package com.example.sp.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopProductDTO {
    private Integer idSp;
    private String maSp;
    private String tenSp;
    private String moTa;
    private String hinhAnh;
    private String imageUrl;
    private Boolean trangThai;
    private LocalDateTime ngayTao;
    private BigDecimal giaBanMin;
    private BigDecimal giaBanMax;
    private Integer tongTon;
    private ShopLookupDTO chatLieu;
    private ShopLookupDTO xuatXu;
    private String loaiAo;
    private List<ShopLookupDTO> loaiAos;
    private List<ShopLookupDTO> kichCos;
    private List<ShopLookupDTO> mauSacs;
    private List<ShopLookupDTO> phongCachMacs;
    private List<ShopLookupDTO> kieuDangs;
}
