package com.example.sp.service.tonkho;

import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.repository.sanpham.ChiTietSanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ChiTietSanPhamRepository variantRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    // Kiểm tra điều kiện và tính hợp lệ cho validate online availability.
    public ChiTietSanPham validateOnlineAvailability(Integer variantId, int quantity) {
        ChiTietSanPham variant = lockVariant(variantId);
        validatePositiveQuantity(quantity);
        validateSellable(variant);
        validateAvailable(variant, quantity);
        return variant;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    // Thực hiện xử lý nghiệp vụ của hàm reserve online.
    public ChiTietSanPham reserveOnline(Integer variantId, int quantity) {
        ChiTietSanPham variant = lockVariant(variantId);
        validatePositiveQuantity(quantity);
        validateSellable(variant);
        validateAvailable(variant, quantity);

        variant.setSoLuongGiu(reserved(variant) + quantity);
        touch(variant);
        return variantRepository.save(variant);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    // Thực hiện xử lý nghiệp vụ của hàm confirm online reservation.
    public ChiTietSanPham confirmOnlineReservation(Integer variantId, int quantity) {
        ChiTietSanPham variant = lockVariant(variantId);
        validatePositiveQuantity(quantity);

        int stock = stock(variant);
        int reserved = reserved(variant);
        if (reserved < quantity || stock < quantity) {
            throw new IllegalArgumentException(stockError(variant));
        }

        variant.setSoLuongTon(stock - quantity);
        variant.setSoLuongGiu(reserved - quantity);
        touch(variant);
        return variantRepository.save(variant);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    // Thực hiện xử lý nghiệp vụ của hàm release online reservation.
    public ChiTietSanPham releaseOnlineReservation(Integer variantId, int quantity) {
        ChiTietSanPham variant = lockVariant(variantId);
        validatePositiveQuantity(quantity);

        int reserved = reserved(variant);
        if (reserved < quantity) {
            throw new IllegalStateException("Số lượng đang giữ của sản phẩm "
                    + variant.getMaChiTietSanPham() + " không hợp lệ");
        }

        variant.setSoLuongGiu(reserved - quantity);
        touch(variant);
        return variantRepository.save(variant);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    // Thực hiện xử lý nghiệp vụ của hàm reserve at counter.
    public ChiTietSanPham reserveAtCounter(Integer variantId, int quantity) {
        ChiTietSanPham variant = lockVariant(variantId);
        validatePositiveQuantity(quantity);
        validateSellable(variant);
        validateAvailable(variant, quantity);

        variant.setSoLuongTon(stock(variant) - quantity);
        touch(variant);
        return variantRepository.save(variant);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    // Thực hiện xử lý nghiệp vụ của hàm deduct online stock.
    public ChiTietSanPham deductOnlineStock(Integer variantId, int quantity) {
        ChiTietSanPham variant = lockVariant(variantId);
        validatePositiveQuantity(quantity);
        validateSellable(variant);
        validateAvailable(variant, quantity);

        variant.setSoLuongTon(stock(variant) - quantity);
        touch(variant);
        return variantRepository.save(variant);
    }

    /**
     * Used when confirming an order created before online reservations were introduced.
     * Online reservations of newer orders remain protected by the availability check.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    // Thực hiện xử lý nghiệp vụ của hàm deduct legacy online stock.
    public ChiTietSanPham deductLegacyOnlineStock(Integer variantId, int quantity) {
        return deductOnlineStock(variantId, quantity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    // Thực hiện xử lý nghiệp vụ của hàm restore stock.
    public ChiTietSanPham restoreStock(Integer variantId, int quantity) {
        ChiTietSanPham variant = lockVariant(variantId);
        validatePositiveQuantity(quantity);

        variant.setSoLuongTon(stock(variant) + quantity);
        touch(variant);
        return variantRepository.save(variant);
    }

    // Thực hiện xử lý nghiệp vụ của hàm lock variant.
    private ChiTietSanPham lockVariant(Integer variantId) {
        if (variantId == null) {
            throw new IllegalArgumentException("Thiếu biến thể sản phẩm");
        }
        return variantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm id=" + variantId));
    }

    // Kiểm tra điều kiện và tính hợp lệ cho validate sellable.
    private void validateSellable(ChiTietSanPham variant) {
        if (!Boolean.TRUE.equals(variant.getTrangThai())
                || variant.getSanPham() == null
                || !Boolean.TRUE.equals(variant.getSanPham().getTrangThai())) {
            throw new IllegalArgumentException("Sản phẩm đã ngừng bán");
        }
    }

    // Kiểm tra điều kiện và tính hợp lệ cho validate available.
    private void validateAvailable(ChiTietSanPham variant, int quantity) {
        if (available(variant) < quantity) {
            throw new IllegalArgumentException(stockError(variant));
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm stock error.
    private String stockError(ChiTietSanPham variant) {
        return "Sản phẩm " + variant.getMaChiTietSanPham()
                + " chỉ còn " + available(variant) + " sản phẩm có thể bán";
    }

    // Kiểm tra điều kiện và tính hợp lệ cho validate positive quantity.
    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm stock.
    private int stock(ChiTietSanPham variant) {
        return variant.getSoLuongTon() == null ? 0 : variant.getSoLuongTon();
    }

    // Thực hiện xử lý nghiệp vụ của hàm reserved.
    private int reserved(ChiTietSanPham variant) {
        return variant.getSoLuongGiu() == null ? 0 : variant.getSoLuongGiu();
    }

    // Thực hiện xử lý nghiệp vụ của hàm available.
    private int available(ChiTietSanPham variant) {
        return Math.max(stock(variant) - reserved(variant), 0);
    }

    // Thực hiện xử lý nghiệp vụ của hàm touch.
    private void touch(ChiTietSanPham variant) {
        variant.setNgayCapNhat(LocalDateTime.now());
    }
}
