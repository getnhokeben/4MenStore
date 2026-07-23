package com.example.sp.model.cuahang;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shop_chat_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_session")
    private Integer id;

    @Column(name = "session_key", nullable = false, unique = true, length = 120)
    private String sessionKey;

    @Column(name = "id_kh")
    private Integer idKhachHang;

    @Column(name = "ten_khach_hang", length = 255)
    private String tenKhachHang;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "trang_thai", length = 30)
    private String trangThai;

    @Column(name = "id_nhan_vien")
    private Integer idNhanVien;

    @Column(name = "ten_nhan_vien", length = 255)
    private String tenNhanVien;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "ngay_dong")
    private LocalDateTime ngayDong;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayTao == null) ngayTao = now;
        if (ngayCapNhat == null) ngayCapNhat = now;
        if (trangThai == null || trangThai.isBlank()) trangThai = "AI";
    }

    @PreUpdate
    public void preUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }
}
