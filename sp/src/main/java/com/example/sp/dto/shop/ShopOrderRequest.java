package com.example.sp.dto.shop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ShopOrderRequest {
    @NotBlank(message = "Vui lòng nhập họ tên")
    private String tenKhachHang;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    private String soDienThoai;

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    private String diaChiKhachHang;

    private String ghiChu;
    private String phuongThucThanhToan;
    private Integer idVoucher;
    private String maVoucher;
    private BigDecimal phiVanChuyen;

    @Valid
    @NotEmpty(message = "Giỏ hàng đang trống")
    private List<ShopOrderItemRequest> items;
}
