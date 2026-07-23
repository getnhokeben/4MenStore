    package com.example.sp.dto.khuyenmai;

    import java.math.BigDecimal;
    import java.time.LocalDateTime;

    public interface SanPhamChiTietPromotionView {
        Integer getIdSpct();
        String getMaSpct();
        String getTenSp();
        String getTenMauSac();
        String getTenKichCo();
        BigDecimal getGiaBan();
        Integer getSoLuongTon();
        String getHinhAnh();
        LocalDateTime getNgayTao();
    }
