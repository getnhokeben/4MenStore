package com.example.sp.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopCustomerDTO {
    private Integer id;
    private String maKh;
    private String tenKhachHang;
    private String tenTaiKhoan;
    private String soDienThoai;
    private String email;
    private String diaChi;
}
