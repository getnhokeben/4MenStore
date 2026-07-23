package com.example.sp.dto.cuahang;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ShopOrderRequest {
    @NotBlank(message = "Vui long nhap ho ten")
    private String tenKhachHang;

    @NotBlank(message = "Vui long nhap so dien thoai")
    private String soDienThoai;

    @Email(message = "Email khong hop le")
    @NotBlank(message = "Vui long nhap email nhan thong tin don hang")
    private String email;

    @NotBlank(message = "Vui long nhap dia chi giao hang")
    private String diaChiKhachHang;

    private String ghiChu;
    private String phuongThucThanhToan;
    private Integer idVoucher;
    private String maVoucher;
    private BigDecimal phiVanChuyen;

    @Valid
    @NotEmpty(message = "Gio hang dang trong")
    private List<ShopOrderItemRequest> items;
}
