package com.example.sp.dto;



import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class ChiTietSanPhamRequest {
    private Integer idSpct; 

    @NotBlank(message = "Mã chi tiết sản phẩm không được trống")
    private String maChiTietSanPham;

    @NotNull(message = "Vui lòng chọn kích cỡ") private Integer idKichCo;
    @NotNull(message = "Vui lòng chọn màu sắc") private Integer idMauSac;
    @NotNull(message = "Vui lòng chọn loại áo") private Integer idLoaiAo;
    @NotNull(message = "Vui lòng chọn phong cách") private Integer idPhongCachMac;
    @NotNull(message = "Vui lòng chọn kiểu dáng") private Integer idKieuDang;

    @NotNull(message = "Số lượng không được trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer soLuongTon;

    @NotNull(message = "Giá nhập không được trống")
    @DecimalMin(value = "0.0", message = "Đơn giá không được nhỏ hơn 0")
    private BigDecimal donGia;

}