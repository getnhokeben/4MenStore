package com.example.sp.dto.pos;

import lombok.Data;

@Data
public class PosCustomerRequest {
    private Integer idKh;
    private String tenKhachHang;
    private String soDienThoai;
    private String diaChiKhachHang;
}
