package com.example.sp.repository.khuyenmai;

import com.example.sp.model.khuyenmai.DotGiamGia;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import com.example.sp.service.khuyenmai.DotGiamGiaService;
import com.example.sp.service.khuyenmai.PhieuGiamGiaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PromotionNameSearchIntegrationTest {

    @Autowired
    private DotGiamGiaRepository promotionRepository;

    @Autowired
    private PhieuGiamGiaRepository voucherRepository;

    @Autowired
    private DotGiamGiaService promotionService;

    @Autowired
    private PhieuGiamGiaService voucherService;

    @Test
    void searchesPromotionByVietnameseName() {
        DotGiamGia promotion = promotionRepository.saveAndFlush(DotGiamGia.builder()
                .maDotGiamGia("DGG_SEARCH_NAME_TEST")
                .tenDotGiamGia("Ưu đãi mùa hè đặc biệt")
                .loaiGiamGia("PHAN_TRAM")
                .giaTriGiamGia(BigDecimal.TEN)
                .ngayBatDau(LocalDateTime.now().minusDays(1))
                .ngayKetThuc(LocalDateTime.now().plusDays(1))
                .trangThai(true)
                .build());

        var result = promotionService.getAll(
                "mua he",
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertTrue(result.stream().anyMatch(item -> item.getId().equals(promotion.getId())));
    }

    @Test
    void searchesVoucherByVietnameseName() {
        PhieuGiamGia voucher = voucherRepository.saveAndFlush(PhieuGiamGia.builder()
                .maPgg("PGG_SEARCH_NAME_TEST")
                .tenPgg("Phiếu ưu đãi mùa hè")
                .loaiGiam("PHAN_TRAM")
                .giaTri(BigDecimal.TEN)
                .dieuKienDonHang(BigDecimal.ZERO)
                .ngayBatDau(LocalDateTime.now().minusDays(1))
                .ngayKetThuc(LocalDateTime.now().plusDays(1))
                .soLuong(10)
                .soLuongDaDung(0)
                .trangThai(true)
                .build());

        var result = voucherService.getAll(
                "mÃ¹a hÃ¨",
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertTrue(result.stream().anyMatch(item -> item.getId().equals(voucher.getId())));
    }
}
