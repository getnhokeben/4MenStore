package com.example.sp.dto.hoadon;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * The complete editable state of an invoice before it is confirmed for fulfilment.
 * Sending the final item list makes adding, removing and changing quantities one
 * atomic operation instead of several partially-completed requests.
 */
@Data
public class CapNhatHoaDonRequest {

    @Size(max = 500, message = "Địa chỉ giao hàng không được vượt quá 500 ký tự")
    private String diaChiKhachHang;

    @Size(max = 255, message = "Tên khách hàng không được vượt quá 255 ký tự")
    private String tenKhachHang;

    @Size(max = 10, message = "Số điện thoại không được vượt quá 10 ký tự")
    private String soDienThoai;

    @NotEmpty(message = "Hóa đơn phải có ít nhất một sản phẩm")
    @Valid
    private List<SanPham> sanPhams;

    @Data
    public static class SanPham {
        @NotNull(message = "Thiếu biến thể sản phẩm")
        private Integer idSpct;

        @NotNull(message = "Thiếu số lượng sản phẩm")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer soLuong;
    }
}
