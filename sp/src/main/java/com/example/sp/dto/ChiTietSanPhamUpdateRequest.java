package com.example.sp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChiTietSanPhamUpdateRequest {
    private String maChiTietSanPham;
    private Integer idKichCo;
    private Integer idMauSac;
    private Integer idLoaiAo;
    private Integer idPhongCachMac;
    private Integer idKieuDang;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer soLuongTon;

    @DecimalMin(value = "0.0", message = "Đơn giá không được nhỏ hơn 0")
    private BigDecimal donGia;

    private Boolean trangThai;
    private String nguoiThucHien;
}
