package com.example.sp.dto.banhang;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PosShippingRequest {
    private Boolean giaoHang;
    private String tenKhachHang;
    private String soDienThoai;
    private String diaChiCuThe;
    private String provinceName;
    private String districtName;
    private String wardName;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
    private String ghiChu;
    private BigDecimal phiVanChuyen;
}
