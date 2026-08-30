package com.example.sp.service.trienkhai;

import com.example.sp.dto.hoadon.CapNhatHoaDonRequest;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.HoaDonChiTiet;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoaDonServiceImplInvoiceEditTest {

    @Mock private HoaDonRepository hoaDonRepo;
    @Mock private HoaDonChiTietRepository chiTietRepo;
    @Mock private PhieuGiamGiaRepository voucherRepo;
    @Mock private LichSuThanhToanRepository lichSuRepo;
    @Mock private KhachHangRepository khachHangRepo;
    @Mock private ThanhToanRepository thanhToanRepo;
    @Mock private PhuongThucThanhToanRepository ptttRepo;
    @Mock private NhanVienRepository nhanVienRepo;
    @Mock private OrderStatusMailService orderStatusMailService;
    @Mock private InventoryService inventoryService;

    @InjectMocks private HoaDonServiceImpl service;

    @Test
    void editingDeliveryOrderRecalculatesAddressShippingAndTotal() {
        HoaDon order = order("Trực tuyến", "Chờ xác nhận");
        HoaDonChiTiet line = line(order, variant(11, "100000"), 1);
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(chiTietRepo.findByHoaDon_Id(1)).thenReturn(List.of(line));
        when(chiTietRepo.save(any(HoaDonChiTiet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDon updated = service.capNhatHoaDonChoXuLy(1, request("Hà Nội", item(11, 1)));

        assertEquals("Hà Nội", updated.getDiaChiKhachHang());
        assertEquals(new BigDecimal("25000"), updated.getPhiVanChuyen());
        assertEquals(new BigDecimal("125000"), updated.getTongTienThanhToan());
    }

    @Test
    void editingCounterOrderReservesStockForNewProduct() {
        HoaDon order = order("Tại quầy", "Chờ thanh toán");
        HoaDonChiTiet currentLine = line(order, variant(11, "100000"), 1);
        ChiTietSanPham newVariant = variant(12, "200000");
        AtomicReference<HoaDonChiTiet> addedLine = new AtomicReference<>();
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));
        when(chiTietRepo.findByHoaDon_Id(1))
                .thenReturn(List.of(currentLine))
                .thenAnswer(invocation -> List.of(currentLine, addedLine.get()));
        when(inventoryService.reserveAtCounter(12, 2)).thenReturn(newVariant);
        when(chiTietRepo.save(any(HoaDonChiTiet.class))).thenAnswer(invocation -> {
            HoaDonChiTiet saved = invocation.getArgument(0);
            if (saved.getChiTietSanPham() == newVariant) {
                addedLine.set(saved);
            }
            return saved;
        });
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDon updated = service.capNhatHoaDonChoXuLy(1, request(null, item(11, 1), item(12, 2)));

        verify(inventoryService).reserveAtCounter(12, 2);
        assertEquals(new BigDecimal("500000"), updated.getTongTienThanhToan());
    }

    @Test
    void editingConfirmedOrderIsRejected() {
        HoaDon order = order("Trực tuyến", "Đã xác nhận");
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> service.capNhatHoaDonChoXuLy(1, request("Hà Nội", item(11, 1))));
    }

    @Test
    void paidOrderCreatesSeparateFollowUpOrderWithCopiedDeliveryDetails() {
        HoaDon source = order("Trực tuyến", "Hoàn thành");
        source.setMaHoaDon("HD-OLD");
        source.setTenKhachHang("Nguyễn An");
        source.setSoDienThoai("0901234567");
        source.setDiaChiKhachHang("Hà Nội");
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(source));
        when(orderStatusMailService.resolveCustomerEmail(source)).thenReturn("an@example.com");
        when(hoaDonRepo.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoaDon created = service.taoDonMuaThem(1);

        assertEquals("Trực tuyến", created.getLoaiDon());
        assertEquals("Chờ xác nhận", created.getTrangThai());
        assertEquals("Hà Nội", created.getDiaChiKhachHang());
        assertEquals("Nguyễn An", created.getTenKhachHang());
        assertEquals(BigDecimal.ZERO, created.getTongTienThanhToan());
        org.junit.jupiter.api.Assertions.assertTrue(created.getGhiChu().contains("HD-OLD"));
        org.junit.jupiter.api.Assertions.assertTrue(created.getGhiChu().contains("CUSTOMER_EMAIL:an@example.com"));
    }

    @Test
    void followUpOrderRequiresCompletedPayment() {
        HoaDon source = order("Tại quầy", "Chờ thanh toán");
        when(hoaDonRepo.findByIdForUpdate(1)).thenReturn(java.util.Optional.of(source));

        assertThrows(IllegalStateException.class, () -> service.taoDonMuaThem(1));
    }

    private HoaDon order(String type, String status) {
        return HoaDon.builder()
                .id(1)
                .loaiDon(type)
                .hinhThucNhanHang("Trực tuyến".equals(type) ? "Giao hàng" : "Tại quầy")
                .trangThai(status)
                .phiVanChuyen(BigDecimal.ZERO)
                .build();
    }

    private HoaDonChiTiet line(HoaDon order, ChiTietSanPham variant, int quantity) {
        BigDecimal total = variant.getDonGia().multiply(BigDecimal.valueOf(quantity));
        return HoaDonChiTiet.builder()
                .hoaDon(order)
                .chiTietSanPham(variant)
                .soLuong(quantity)
                .donGia(variant.getDonGia())
                .thanhTien(total)
                .build();
    }

    private ChiTietSanPham variant(int id, String price) {
        ChiTietSanPham variant = new ChiTietSanPham();
        variant.setIdSpct(id);
        variant.setDonGia(new BigDecimal(price));
        return variant;
    }

    private CapNhatHoaDonRequest request(String address, CapNhatHoaDonRequest.SanPham... items) {
        CapNhatHoaDonRequest request = new CapNhatHoaDonRequest();
        request.setDiaChiKhachHang(address);
        request.setSanPhams(List.of(items));
        return request;
    }

    private CapNhatHoaDonRequest.SanPham item(int idSpct, int quantity) {
        CapNhatHoaDonRequest.SanPham item = new CapNhatHoaDonRequest.SanPham();
        item.setIdSpct(idSpct);
        item.setSoLuong(quantity);
        return item;
    }
}
