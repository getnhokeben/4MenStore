package com.example.sp.model.hoadon;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "hoa_don")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hoa_don")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia;

    @Column(name = "ma_hoa_don")
    private String maHoaDon;

    @Column(name = "loai_don")
    private String loaiDon;

    @Builder.Default
    @Column(name = "hinh_thuc_nhan_hang")
    private String hinhThucNhanHang = "Tại quầy";

    @Column(name = "phi_van_chuyen")
    private BigDecimal phiVanChuyen;

    @Column(name = "tong_tien_goc")
    private BigDecimal tongTienGoc;

    @Column(name = "so_tien_giam")
    private BigDecimal soTienGiam;

    @Column(name = "tong_tien_thanh_toan")
    private BigDecimal tongTienThanhToan;

    @Column(name = "ten_khach_hang")
    private String tenKhachHang;

    @Column(name = "dia_chi_khach_hang")
    private String diaChiKhachHang;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "trang_thai")
    private String trangThai;

    @Builder.Default
    @Column(name = "da_giu_ton", nullable = false)
    private Boolean daGiuTon = false;

    /**
     * NULL is retained for legacy invoices so their inventory state can be
     * inferred from the previous status-based flow without rewriting data.
     */
    @Column(name = "da_tru_ton")
    private Boolean daTruTon;

    @Builder.Default
    @Column(name = "da_hoan_ton", nullable = false)
    private Boolean daHoanTon = false;

    @Column(name = "ly_do_hoan_hang")
    private String lyDoHoanHang;

    @Column(name = "ghi_chu_hoan_hang")
    private String ghiChuHoanHang;

    @Column(name = "ngay_yeu_cau_hoan")
    private LocalDateTime ngayYeuCauHoan;

    @Column(name = "ngay_nhan_hang_hoan")
    private LocalDateTime ngayNhanHangHoan;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime ngayThanhToan;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;
}
