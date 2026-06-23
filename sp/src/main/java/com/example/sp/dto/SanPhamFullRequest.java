package com.example.sp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class SanPhamFullRequest {
    @NotBlank(message = "Mã sản phẩm không được để trống")
    private String maSp;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String tenSp;

    // Các trường nhập text thay vì ID
    private String tenXuatXu;      // tên xuất xứ
    private String tenChatLieu;    // tên chất liệu

    // Vẫn giữ ID để tương thích (có thể null)
    private Integer idXuatXu;
    private Integer idChatLieu;

    private String moTa;
    private String hinhAnhChinh;
    private String nguoiThucHien;

    private List<String> danhSachHinhAnhPhu;

    @NotEmpty(message = "Sản phẩm phải có ít nhất một biến thể chi tiết")
    @Valid
    private List<ChiTietSanPhamRequest> danhSachBienThe;
}