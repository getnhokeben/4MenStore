package com.example.sp.model.khachhang;

import com.example.sp.validation.CustomerNameValidator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "khach_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kh")
    private Integer id;

    @Column(name = "ma_kh", length = 50, unique = true)
    private String maKh;

    @NotBlank(message = "Tên khách hàng không được để trống")
    @Size(max = 255, message = "Tên khách hàng không được vượt quá 255 ký tự")
    @Pattern(
            regexp = CustomerNameValidator.PATTERN,
            message = CustomerNameValidator.INVALID_MESSAGE
    )
    @Column(name = "ten_khach_hang", length = 255)
    private String tenKhachHang;
    @Column(name = "ten_tai_khoan", length = 255)
    private String tenTaiKhoan;

    @Pattern(
            regexp = "^(03|05|07|08|09)\\d{8}$",
            message = "Số điện thoại không hợp lệ"
    )
    @Column(name = "so_dien_thoai", length = 255)
    private String soDienThoai;

    @Email(message = "Email không đúng định dạng")
    @Column(name = "email", length = 255)
    private String email;

    // Cột cũ, đồng bộ bằng địa chỉ mặc định.
    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    @Pattern(
            regexp = "^(Nam|Nữ|Khác)?$",
            message = "Giới tính chỉ có thể là Nam, Nữ hoặc Khác"
    )
    @Column(name = "gioi_tinh", length = 255)
    private String gioiTinh;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "mat_khau", length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String matKhau;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Pattern(
            regexp = "^\\d{12}$",
            message = "CCCD phải gồm đúng 12 chữ số"
    )
    @Column(name = "cccd", length = 255)
    private String cccd;

    @Column(name = "dia_chi_chi_tiet", length = 255)
    private String diaChiChiTiet;

    @Column(name = "phuong_xa", length = 255)
    private String phuongXa;

    @Column(name = "phuong_xa_code")
    private Integer phuongXaCode;

    @Column(name = "quan_huyen", length = 255)
    private String quanHuyen;

    @Column(name = "quan_huyen_code")
    private Integer quanHuyenCode;

    @Column(name = "tinh_thanh", length = 255)
    private String tinhThanh;

    @Column(name = "tinh_thanh_code")
    private Integer tinhThanhCode;

    @Transient
    // Tải hoặc truy xuất dữ liệu cho get dia chi display.
    public String getDiaChiDisplay() {
        if (diaChi != null && !diaChi.isBlank()) {
            return diaChi;
        }

        StringBuilder result = new StringBuilder();

        appendAddressPart(result, diaChiChiTiet);
        appendAddressPart(result, phuongXa);
        appendAddressPart(result, tinhThanh);

        return result.toString();
    }

    // Thực hiện xử lý nghiệp vụ của hàm append address part.
    private void appendAddressPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(", ");
        }

        builder.append(value);
    }

    @PrePersist
    // Thực hiện xử lý nghiệp vụ của hàm pre persist.
    public void prePersist() {
        if (trangThai == null) {
            trangThai = true;
        }
    }
}
