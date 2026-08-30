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

    @Column(name = "pgg_snapshot_ma", length = 100)
    private String pggSnapshotMa;

    @Column(name = "pgg_snapshot_ten", length = 255)
    private String pggSnapshotTen;

    @Column(name = "pgg_snapshot_loai", length = 50)
    private String pggSnapshotLoai;

    @Column(name = "pgg_snapshot_gia_tri")
    private BigDecimal pggSnapshotGiaTri;

    @Column(name = "pgg_snapshot_gia_tri_toi_da")
    private BigDecimal pggSnapshotGiaTriToiDa;

    @Column(name = "pgg_snapshot_dieu_kien")
    private BigDecimal pggSnapshotDieuKien;

    @Column(name = "pgg_snapshot_so_tien_giam")
    private BigDecimal pggSnapshotSoTienGiam;

    // Thực hiện xử lý nghiệp vụ của hàm capture voucher snapshot.
    public void captureVoucherSnapshot() {
        if (pggSnapshotMa != null || phieuGiamGia == null) {
            return;
        }
        pggSnapshotMa = phieuGiamGia.getMaPgg();
        pggSnapshotTen = phieuGiamGia.getTenPgg();
        pggSnapshotLoai = phieuGiamGia.getLoaiGiam();
        pggSnapshotGiaTri = phieuGiamGia.getGiaTri();
        pggSnapshotGiaTriToiDa = phieuGiamGia.getGiaTriToiDa();
        pggSnapshotDieuKien = phieuGiamGia.getDieuKienDonHang();
        pggSnapshotSoTienGiam = soTienGiam;
    }

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime ngayThanhToan;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;
}
