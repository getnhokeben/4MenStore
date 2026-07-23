package com.example.sp.service.tonkho;

import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.model.sanpham.SanPham;
import com.example.sp.repository.sanpham.ChiTietSanPhamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private final ChiTietSanPhamRepository repository = mock(ChiTietSanPhamRepository.class);
    private final InventoryService service = new InventoryService(repository);
    private ChiTietSanPham variant;

    @BeforeEach
    void setUp() {
        SanPham product = new SanPham();
        product.setTrangThai(true);

        variant = new ChiTietSanPham();
        variant.setIdSpct(7);
        variant.setMaChiTietSanPham("SPCT-7");
        variant.setSanPham(product);
        variant.setTrangThai(true);
        variant.setSoLuongTon(10);
        variant.setSoLuongGiu(0);

        when(repository.findByIdForUpdate(7)).thenReturn(Optional.of(variant));
        when(repository.save(any(ChiTietSanPham.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void onlineReservationProtectsStockFromCounterSaleThenConvertsOnConfirmation() {
        service.reserveOnline(7, 6);

        assertEquals(10, variant.getSoLuongTon());
        assertEquals(6, variant.getSoLuongGiu());
        assertEquals(4, variant.getSoLuongKhaDung());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.reserveAtCounter(7, 6)
        );
        assertEquals("Sản phẩm SPCT-7 chỉ còn 4 sản phẩm có thể bán", error.getMessage());

        service.reserveAtCounter(7, 4);
        assertEquals(6, variant.getSoLuongTon());
        assertEquals(6, variant.getSoLuongGiu());
        assertEquals(0, variant.getSoLuongKhaDung());

        service.confirmOnlineReservation(7, 6);
        assertEquals(0, variant.getSoLuongTon());
        assertEquals(0, variant.getSoLuongGiu());
        assertEquals(0, variant.getSoLuongKhaDung());
    }

    @Test
    void cancellingPendingOnlineOrderReturnsReservedQuantityWithoutChangingPhysicalStock() {
        service.reserveOnline(7, 6);
        service.releaseOnlineReservation(7, 6);

        assertEquals(10, variant.getSoLuongTon());
        assertEquals(0, variant.getSoLuongGiu());
        assertEquals(10, variant.getSoLuongKhaDung());
    }

    @Test
    void legacyOrderCannotConsumeStockReservedByANewerOnlineOrder() {
        service.reserveOnline(7, 6);

        assertThrows(IllegalArgumentException.class, () -> service.deductLegacyOnlineStock(7, 6));
        assertEquals(10, variant.getSoLuongTon());
        assertEquals(6, variant.getSoLuongGiu());
        assertEquals(4, variant.getSoLuongKhaDung());
    }
}
