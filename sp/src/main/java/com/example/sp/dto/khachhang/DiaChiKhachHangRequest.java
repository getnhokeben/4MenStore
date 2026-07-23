package com.example.sp.dto.khachhang;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaChiKhachHangRequest {

    @NotBlank(message = "Vui lòng nhập tên địa chỉ")
    @Size(max = 255, message = "Tên địa chỉ không được vượt quá 255 ký tự")
    private String tenDiaChi;

    @NotBlank(message = "Vui lòng nhập tên người nhận")
    @Size(max = 255, message = "Tên người nhận không được vượt quá 255 ký tự")
    private String tenNguoiNhan;

    @NotBlank(message = "Vui lòng nhập số điện thoại người nhận")
    @Pattern(
            regexp = "^(03|05|07|08|09)\\d{8}$",
            message = "Số điện thoại người nhận không hợp lệ"
    )
    private String soDienThoai;

    @NotBlank(message = "Vui lòng chọn Tỉnh / Thành")
    @Size(max = 255)
    private String thanhPho;

    @Size(max = 255)
    private String quan;

    @NotBlank(message = "Vui lòng chọn Phường / Xã")
    @Size(max = 255)
    private String phuong;

    @NotBlank(message = "Vui lòng nhập số nhà / tên đường")
    @Size(max = 255)
    private String diaChiCuThe;

    private Boolean macDinh;
}
