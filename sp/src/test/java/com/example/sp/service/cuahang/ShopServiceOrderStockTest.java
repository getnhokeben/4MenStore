package com.example.sp.service.cuahang;

import com.example.sp.dto.cuahang.ShopOrderItemRequest;
import com.example.sp.dto.cuahang.ShopOrderRequest;
import com.example.sp.dto.cuahang.ShopOrderResponse;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.HoaDonChiTiet;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.repository.hoadon.HoaDonChiTietRepository;
import com.example.sp.repository.hoadon.HoaDonRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.khuyenmai.ChiTietDotGiamGiaRepository;
import com.example.sp.repository.khuyenmai.PhieuGiamGiaRepository;
import com.example.sp.repository.sanpham.ChiTietSanPhamRepository;
import com.example.sp.repository.sanpham.HinhAnhSanPhamRepository;
import com.example.sp.service.tonkho.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopServiceOrderStockTest {

    private final ChiTietSanPhamRepository variantRepository = mock(ChiTietSanPhamRepository.class);
    private final HinhAnhSanPhamRepository imageRepository = mock(HinhAnhSanPhamRepository.class);
    private final HoaDonRepository orderRepository = mock(HoaDonRepository.class);
    private final HoaDonChiTietRepository orderItemRepository = mock(HoaDonChiTietRepository.class);
    private final PhieuGiamGiaRepository voucherRepository = mock(PhieuGiamGiaRepository.class);
    private final KhachHangRepository customerRepository = mock(KhachHangRepository.class);
    private final ChiTietDotGiamGiaRepository campaignRepository = mock(ChiTietDotGiamGiaRepository.class);
    private final ShopOrderMailService mailService = mock(ShopOrderMailService.class);
    private final InventoryService inventoryService = mock(InventoryService.class);
    private ShopService service;

    @BeforeEach
    void setUp() {
        service = new ShopService(
                variantRepository,
                imageRepository,
                orderRepository,
                orderItemRepository,
                voucherRepository,
                customerRepository,
                campaignRepository,
                mailService,
                inventoryService
        );
    }

    @Test
    void creatingGatewayOrderOnlyValidatesStockAndLeavesInventoryUntouched() {
        ChiTietSanPham variant = new ChiTietSanPham();
        variant.setIdSpct(101);
        variant.setDonGia(new BigDecimal("200000"));
        when(inventoryService.validateOnlineAvailability(101, 2)).thenReturn(variant);
        when(campaignRepository.findActivePromotionsByVariantId(
                org.mockito.ArgumentMatchers.eq(101), any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> {
            HoaDon order = invocation.getArgument(0);
            order.setId(42);
            return order;
        });
        when(orderItemRepository.save(any(HoaDonChiTiet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByHoaDon_Id(42)).thenReturn(List.of());

        ShopOrderResponse response = service.createOrder(gatewayOrderRequest(), null);

        assertEquals("Chờ thanh toán online", response.getTrangThai());
        verify(inventoryService).validateOnlineAvailability(101, 2);
        verify(inventoryService, never()).reserveOnline(101, 2);
        verify(inventoryService, never()).deductOnlineStock(101, 2);

        ArgumentCaptor<HoaDon> orderCaptor = ArgumentCaptor.forClass(HoaDon.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(new BigDecimal("35000"), orderCaptor.getValue().getPhiVanChuyen());
        assertFalse(Boolean.TRUE.equals(orderCaptor.getValue().getDaGiuTon()));
        assertFalse(Boolean.TRUE.equals(orderCaptor.getValue().getDaTruTon()));
    }

    @Test
    void allowsGuestCheckoutToUsePublicVoucher() {
        ChiTietSanPham variant = new ChiTietSanPham();
        variant.setIdSpct(101);
        variant.setDonGia(new BigDecimal("200000"));
        when(inventoryService.validateOnlineAvailability(101, 2)).thenReturn(variant);
        when(campaignRepository.findActivePromotionsByVariantId(
                org.mockito.ArgumentMatchers.eq(101), any(LocalDateTime.class)
        )).thenReturn(List.of());

        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setId(9);
        voucher.setTrangThai(true);
        voucher.setSoLuong(10);
        voucher.setSoLuongDaDung(0);
        voucher.setLoaiGiam("PHAN_TRAM");
        voucher.setGiaTri(new BigDecimal("10"));
        when(voucherRepository.findById(9)).thenReturn(java.util.Optional.of(voucher));
        when(voucherRepository.findByIdForUpdate(9)).thenReturn(java.util.Optional.of(voucher));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> {
            HoaDon order = invocation.getArgument(0);
            order.setId(42);
            return order;
        });
        when(orderItemRepository.save(any(HoaDonChiTiet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByHoaDon_Id(42)).thenReturn(List.of());

        ShopOrderRequest request = gatewayOrderRequest();
        request.setIdVoucher(9);

        service.createOrder(request, null);

        assertEquals(1, voucher.getSoLuongDaDung());
        verify(voucherRepository).save(voucher);
    }

    private ShopOrderRequest gatewayOrderRequest() {
        ShopOrderItemRequest item = new ShopOrderItemRequest();
        item.setIdSpct(101);
        item.setSoLuong(2);

        ShopOrderRequest request = new ShopOrderRequest();
        request.setTenKhachHang("Nguyễn Văn Anh");
        request.setSoDienThoai("0982222229");
        request.setEmail("anh@example.com");
        request.setDiaChiKhachHang("Hội Hợp, Vĩnh Phúc");
        request.setPhuongThucThanhToan("VNPAY");
        request.setPhiVanChuyen(BigDecimal.ZERO);
        request.setItems(List.of(item));
        return request;
    }
}
