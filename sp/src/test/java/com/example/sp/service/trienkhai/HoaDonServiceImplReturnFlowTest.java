package com.example.sp.service.trienkhai;

import com.example.sp.dto.hoadon.ChiTietHoanKhoRequest;
import com.example.sp.dto.hoadon.XacNhanHoanHangRequest;
import com.example.sp.dto.hoadon.XuLyGiaoHangThatBaiRequest;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.HoaDonChiTiet;
import com.example.sp.model.hoadon.LichSuThanhToan;
import com.example.sp.model.hoadon.ThanhToan;
import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.repository.hoadon.HoaDonChiTietRepository;
import com.example.sp.repository.hoadon.HoaDonRepository;
import com.example.sp.repository.hoadon.LichSuThanhToanRepository;
import com.example.sp.repository.hoadon.PhuongThucThanhToanRepository;
import com.example.sp.repository.hoadon.ThanhToanRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.khuyenmai.PhieuGiamGiaRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.service.hoadon.OrderStatusMailService;
import com.example.sp.service.tonkho.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoaDonServiceImplReturnFlowTest {

    @Mock
    private HoaDonRepository hoaDonRepo;
    @Mock
    private HoaDonChiTietRepository chiTietRepo;
    @Mock
    private PhieuGiamGiaRepository voucherRepo;
    @Mock
    private LichSuThanhToanRepository lichSuRepo;
    @Mock
    private KhachHangRepository khachHangRepo;
    @Mock
    private ThanhToanRepository thanhToanRepo;
    @Mock
    private PhuongThucThanhToanRepository ptttRepo;
    @Mock
    private NhanVienRepository nhanVienRepo;
    @Mock
    private OrderStatusMailService orderStatusMailService;
    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private HoaDonServiceImpl service;

    @Test
    void cancellingOrderInTransitMovesToWaitingReturnWithoutRestoringStock() {
        HoaDon order = deliveryOrder("Đang giao hàng");
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDon updated = service.huyHoaDon(1);

        assertEquals("Chờ hàng hoàn", updated.getTrangThai());
        assertEquals("Khách không nhận hàng", updated.getLyDoHoanHang());
        assertNotNull(updated.getNgayYeuCauHoan());
        assertFalse(Boolean.TRUE.equals(updated.getDaHoanTon()));
        verify(inventoryService, never()).restoreStock(anyInt(), anyInt());
        verify(chiTietRepo, never()).findByHoaDon_Id(anyInt());
    }

    @Test
    void carrierLostOrderIsCancelledWithoutRestoringStock() {
        HoaDon order = deliveryOrder("Đang giao hàng");
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        XuLyGiaoHangThatBaiRequest request = incident("MAT_HANG_VAN_CHUYEN", "Mã biên bản VC-001");
        HoaDon updated = service.xuLyGiaoHangThatBai(1, request);

        assertEquals("Đã hủy", updated.getTrangThai());
        assertEquals("Đơn vị vận chuyển làm mất hàng", updated.getLyDoHoanHang());
        assertEquals("Mã biên bản VC-001", updated.getGhiChuHoanHang());
        assertFalse(Boolean.TRUE.equals(updated.getDaHoanTon()));
        verify(inventoryService, never()).restoreStock(anyInt(), anyInt());
        verify(chiTietRepo, never()).findByHoaDon_Id(anyInt());
    }

    @Test
    void carrierCompensationCompletesLostOrderAndRecordsPayment() {
        HoaDon order = deliveryOrder("Đã hủy");
        order.setLyDoHoanHang("Đơn vị vận chuyển làm mất hàng");
        order.setTongTienThanhToan(new BigDecimal("350000"));
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ptttRepo.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());

        HoaDon updated = service.xacNhanDonViVanChuyenDenBu(1);

        assertEquals("Hoàn thành", updated.getTrangThai());
        assertNotNull(updated.getNgayThanhToan());
        assertTrue(updated.getGhiChuHoanHang().contains("đền bù"));
        verify(thanhToanRepo).save(any(ThanhToan.class));
        verify(lichSuRepo).save(any(LichSuThanhToan.class));
    }

    @Test
    void customerRefusalWaitsForPhysicalReturnBeforeRestocking() {
        HoaDon order = deliveryOrder("Đang giao hàng");
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDon updated = service.xuLyGiaoHangThatBai(
                1,
                incident("KHACH_KHONG_NHAN", "Đã gọi khách xác nhận từ chối nhận")
        );

        assertEquals("Chờ hàng hoàn", updated.getTrangThai());
        assertEquals("Khách từ chối nhận hàng / bom hàng", updated.getLyDoHoanHang());
        assertNotNull(updated.getNgayYeuCauHoan());
        assertFalse(Boolean.TRUE.equals(updated.getDaHoanTon()));
        verify(inventoryService, never()).restoreStock(anyInt(), anyInt());
    }

    @Test
    void wrongProductWaitsForOldParcelToReturnBeforeRestocking() {
        HoaDon order = deliveryOrder("Đang giao hàng");
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDon updated = service.xuLyGiaoHangThatBai(
                1,
                incident("SHOP_GUI_SAI", "Đã xin lỗi khách, cần tạo đơn giao đúng")
        );

        assertEquals("Chờ hàng hoàn", updated.getTrangThai());
        assertEquals("Shop gửi sai sản phẩm", updated.getLyDoHoanHang());
        assertEquals("Đã xin lỗi khách, cần tạo đơn giao đúng", updated.getGhiChuHoanHang());
        verify(inventoryService, never()).restoreStock(anyInt(), anyInt());
    }

    @Test
    void confirmingReturnedGoodsRestoresOnlySellableQuantity() {
        HoaDon order = deliveryOrder("Chờ hàng hoàn");
        order.setDaHoanTon(false);
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDonChiTiet shirt = orderItem(11, 101, 2);
        HoaDonChiTiet trousers = orderItem(12, 102, 1);
        when(chiTietRepo.findByHoaDon_Id(1)).thenReturn(List.of(shirt, trousers));
        when(chiTietRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        XacNhanHoanHangRequest request = new XacNhanHoanHangRequest();
        request.setGhiChu("Một sản phẩm nguyên vẹn, một sản phẩm bị hỏng");
        request.setChiTiet(List.of(
                new ChiTietHoanKhoRequest(11, 1),
                new ChiTietHoanKhoRequest(12, 0)
        ));

        HoaDon updated = service.xacNhanHangHoan(1, request);

        verify(inventoryService).restoreStock(101, 1);
        verify(inventoryService, never()).restoreStock(eq(102), anyInt());
        assertEquals(1, shirt.getSoLuongHoanKho());
        assertEquals(0, trousers.getSoLuongHoanKho());
        assertTrue(Boolean.TRUE.equals(updated.getDaHoanTon()));
        assertNotNull(updated.getNgayNhanHangHoan());
        assertEquals("Đã hủy", updated.getTrangThai());
    }

    @Test
    void confirmingReturnedGoodsTwiceIsRejectedBeforeInventoryChanges() {
        HoaDon order = deliveryOrder("Chờ hàng hoàn");
        order.setDaHoanTon(true);
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.xacNhanHangHoan(1, new XacNhanHoanHangRequest())
        );

        assertEquals("Đơn hàng đã được hoàn tồn trước đó", error.getMessage());
        verify(inventoryService, never()).restoreStock(anyInt(), anyInt());
    }

    @Test
    void returnedQuantityCannotExceedOrderedQuantity() {
        HoaDon order = deliveryOrder("Chờ hàng hoàn");
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(chiTietRepo.findByHoaDon_Id(1)).thenReturn(List.of(orderItem(11, 101, 2)));

        XacNhanHoanHangRequest request = new XacNhanHoanHangRequest();
        request.setChiTiet(List.of(new ChiTietHoanKhoRequest(11, 3)));

        assertThrows(IllegalArgumentException.class, () -> service.xacNhanHangHoan(1, request));
        verify(inventoryService, never()).restoreStock(anyInt(), anyInt());
    }

    @Test
    void codOrderDeductsStockWhenAdminConfirms() {
        HoaDon order = deliveryOrder("Chờ xác nhận");
        order.setDaTruTon(false);
        HoaDonChiTiet item = orderItem(11, 101, 2);
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(chiTietRepo.findByHoaDon_Id(1)).thenReturn(List.of(item));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDon updated = service.capNhatTrangThai(1, "Đã xác nhận", null);

        verify(inventoryService).deductLegacyOnlineStock(101, 2);
        assertTrue(Boolean.TRUE.equals(updated.getDaTruTon()));
        assertFalse(Boolean.TRUE.equals(updated.getDaGiuTon()));
    }

    @Test
    void onlineOrderCannotSkipConfirmationBeforeStockDeduction() {
        HoaDon order = deliveryOrder("Chờ xác nhận");
        order.setDaTruTon(false);
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.capNhatTrangThai(1, "Đang chuẩn bị hàng", null)
        );

        assertEquals("Phải chuyển đơn sang Đã xác nhận trước khi xử lý tiếp", error.getMessage());
        verify(inventoryService, never()).deductLegacyOnlineStock(anyInt(), anyInt());
        verify(inventoryService, never()).confirmOnlineReservation(anyInt(), anyInt());
        verify(hoaDonRepo, never()).save(any(HoaDon.class));
    }

    @Test
    void prepaidOrderIsNotDeductedAgainWhenAdminConfirms() {
        HoaDon order = deliveryOrder("Chờ xác nhận");
        order.setDaTruTon(true);
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDon updated = service.capNhatTrangThai(1, "Đã xác nhận", null);

        assertEquals("Đã xác nhận", updated.getTrangThai());
        verify(inventoryService, never()).deductLegacyOnlineStock(anyInt(), anyInt());
        verify(inventoryService, never()).confirmOnlineReservation(anyInt(), anyInt());
        verify(chiTietRepo, never()).findByHoaDon_Id(anyInt());
    }

    @Test
    void cancellingPrepaidOrderBeforeShippingRestoresItsStock() {
        HoaDon order = deliveryOrder("Chờ xác nhận");
        order.setDaTruTon(true);
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(chiTietRepo.findByHoaDon_Id(1)).thenReturn(List.of(orderItem(11, 101, 2)));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDon updated = service.capNhatTrangThai(1, "Đã hủy", null);

        verify(inventoryService).restoreStock(101, 2);
        assertFalse(Boolean.TRUE.equals(updated.getDaTruTon()));
    }

    private HoaDon deliveryOrder(String status) {
        return HoaDon.builder()
                .id(1)
                .loaiDon("Trực tuyến")
                .hinhThucNhanHang("Giao hàng")
                .trangThai(status)
                .daGiuTon(false)
                .daHoanTon(false)
                .build();
    }

    private HoaDonChiTiet orderItem(Integer itemId, Integer variantId, Integer quantity) {
        ChiTietSanPham variant = new ChiTietSanPham();
        variant.setIdSpct(variantId);
        return HoaDonChiTiet.builder()
                .id(itemId)
                .chiTietSanPham(variant)
                .soLuong(quantity)
                .soLuongHoanKho(0)
                .build();
    }

    private XuLyGiaoHangThatBaiRequest incident(String type, String note) {
        XuLyGiaoHangThatBaiRequest request = new XuLyGiaoHangThatBaiRequest();
        request.setLoaiSuCo(type);
        request.setGhiChu(note);
        return request;
    }
}
