package com.example.sp.dto.cuahang;

import com.example.sp.validation.CustomerNameValidator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ShopOrderRequest {
    @NotBlank(message = "Vui long nhap ho ten")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    @Pattern(
            regexp = CustomerNameValidator.PATTERN,
            message = CustomerNameValidator.INVALID_MESSAGE
    )
    private String tenKhachHang;

    @NotBlank(message = "Vui long nhap so dien thoai")
    @Pattern(
            regexp = "^(03|05|07|08|09)\\d{8}$",
            message = "Số điện thoại không đúng định dạng Việt Nam"
    )
    private String soDienThoai;

    @Email(message = "Email khong hop le")
    @NotBlank(message = "Vui long nhap email nhan thong tin don hang")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Vui long nhap dia chi giao hang")
    @Size(max = 500, message = "Địa chỉ giao hàng không được vượt quá 500 ký tự")
    private String diaChiKhachHang;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String ghiChu;
    private String phuongThucThanhToan;
    private Integer idVoucher;
    private String maVoucher;

    /**
     * Retained only so older clients can send the previous payload shape.
     * The server always calculates shipping from the order itself.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal phiVanChuyen;

    @Valid
    @NotEmpty(message = "Gio hang dang trong")
    @Size(max = 50, message = "Giỏ hàng tối đa 50 sản phẩm")
    private List<@NotNull(message = "Dòng sản phẩm không hợp lệ") @Valid ShopOrderItemRequest> items;
}
