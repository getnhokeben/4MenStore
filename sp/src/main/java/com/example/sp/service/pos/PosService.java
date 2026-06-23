package com.example.sp.service.pos;

import com.example.sp.dto.pos.PosCheckoutRequest;
import com.example.sp.dto.pos.PosCustomerRequest;
import com.example.sp.dto.pos.PosOrderDTO;
import com.example.sp.dto.pos.PosOrderItemDTO;
import com.example.sp.dto.pos.PosOrderItemRequest;
import com.example.sp.model.ChiTietSanPham;
import com.example.sp.model.HinhAnhSanPham;
import com.example.sp.model.SanPham;
import com.example.sp.model.customer.KhachHang;
import com.example.sp.model.order.HoaDon;
import com.example.sp.model.order.HoaDonChiTiet;
import com.example.sp.model.promotion.PhieuGiamGia;
import com.example.sp.repository.ChiTietSanPhamRepository;
import com.example.sp.repository.HinhAnhSanPhamRepository;
import com.example.sp.repository.HoaDonChiTietRepository;
import com.example.sp.repository.HoaDonRepository;
import com.example.sp.repository.KhachHangRepository;
import com.example.sp.repository.PhieuGiamGiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PosService {

    private static final DateTimeFormatter ORDER_CODE_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepo;
    private final ChiTietSanPhamRepository chiTietSanPhamRepo;
    private final KhachHangRepository khachHangRepo;
    private final PhieuGiamGiaRepository voucherRepo;
    private final HinhAnhSanPhamRepository hinhAnhRepo;

    @Transactional
    public PosOrderDTO createOrder() {
        HoaDon order = HoaDon.builder()
                .maHoaDon(generateOrderCode())
                .loaiDon("Tại quầy")
                .phiVanChuyen(BigDecimal.ZERO)
                .tongTienGoc(BigDecimal.ZERO)
                .soTienGiam(BigDecimal.ZERO)
                .tongTienThanhToan(BigDecimal.ZERO)
                .tenKhachHang("Khách lẻ")
                .trangThai("Chờ thanh toán")
                .ngayTao(LocalDateTime.now())
                .ngayCapNhat(LocalDateTime.now())
                .build();
        return toDTO(hoaDonRepo.save(order), BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public PosOrderDTO getOrder(Integer id) {
        return toDTO(findOrder(id), BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO addItem(Integer idHoaDon, PosOrderItemRequest request) {
        HoaDon order = draftOrder(idHoaDon);
        ChiTietSanPham variant = findSellableVariant(request.getIdSpct());
        int requestedQty = request.getSoLuong();
        HoaDonChiTiet detail = hoaDonChiTietRepo.findByHoaDon_Id(order.getId()).stream()
                .filter(item -> Objects.equals(item.getChiTietSanPham().getIdSpct(), variant.getIdSpct()))
                .findFirst()
                .orElse(null);

        int currentQty = detail == null ? 0 : detail.getSoLuong();
        validateStock(variant, currentQty + requestedQty);

        if (detail == null) {
            detail = HoaDonChiTiet.builder()
                    .hoaDon(order)
                    .chiTietSanPham(variant)
                    .donGia(money(variant.getDonGia()))
                    .soLuong(requestedQty)
                    .build();
        } else {
            detail.setSoLuong(currentQty + requestedQty);
            detail.setDonGia(money(variant.getDonGia()));
        }
        detail.setThanhTien(detail.getDonGia().multiply(BigDecimal.valueOf(detail.getSoLuong())));
        hoaDonChiTietRepo.save(detail);
        recalculate(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO updateItem(Integer idHoaDon, Integer idHdct, Integer soLuong) {
        HoaDon order = draftOrder(idHoaDon);
        if (soLuong == null || soLuong < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
        HoaDonChiTiet detail = findOrderItem(order, idHdct);
        ChiTietSanPham variant = detail.getChiTietSanPham();
        validateStock(variant, soLuong);
        detail.setSoLuong(soLuong);
        detail.setDonGia(money(variant.getDonGia()));
        detail.setThanhTien(detail.getDonGia().multiply(BigDecimal.valueOf(soLuong)));
        hoaDonChiTietRepo.save(detail);
        recalculate(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO removeItem(Integer idHoaDon, Integer idHdct) {
        HoaDon order = draftOrder(idHoaDon);
        HoaDonChiTiet detail = findOrderItem(order, idHdct);
        hoaDonChiTietRepo.delete(detail);
        recalculate(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO setCustomer(Integer idHoaDon, PosCustomerRequest request) {
        HoaDon order = draftOrder(idHoaDon);
        KhachHang customer = null;
        if (request.getIdKh() != null) {
            customer = khachHangRepo.findById(request.getIdKh())
                    .filter(kh -> Boolean.TRUE.equals(kh.getTrangThai()))
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
        }

        String name = firstNonBlank(request.getTenKhachHang(), customer == null ? null : customer.getTenKhachHang());
        String phone = firstNonBlank(request.getSoDienThoai(), customer == null ? null : customer.getSoDienThoai());
        String address = firstNonBlank(request.getDiaChiKhachHang(), customer == null ? null : customer.getDiaChi());

        if (name == null) name = "Khách lẻ";
        if (phone != null && !phone.replaceAll("\\D", "").matches("^(03|05|07|08|09)\\d{8}$")) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam");
        }

        order.setKhachHang(customer);
        order.setTenKhachHang(name);
        order.setSoDienThoai(phone == null ? null : phone.replaceAll("\\D", ""));
        order.setDiaChiKhachHang(address);
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepo.save(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO applyVoucher(Integer idHoaDon, String maVoucher) {
        HoaDon order = draftOrder(idHoaDon);
        BigDecimal subtotal = subtotal(order);
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Đơn hàng chưa có sản phẩm");
        }

        PhieuGiamGia voucher = voucherRepo.findFirstByMaPggIgnoreCase(requireText(maVoucher, "Vui lòng nhập mã giảm giá"))
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại"));
        validateVoucher(voucher, subtotal);
        order.setPhieuGiamGia(voucher);
        recalculate(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO removeVoucher(Integer idHoaDon) {
        HoaDon order = draftOrder(idHoaDon);
        order.setPhieuGiamGia(null);
        recalculate(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO checkout(Integer idHoaDon, PosCheckoutRequest request) {
        HoaDon order = draftOrder(idHoaDon);
        List<HoaDonChiTiet> items = hoaDonChiTietRepo.findByHoaDon_Id(order.getId());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng chưa có sản phẩm");
        }
        recalculate(order);
        BigDecimal paid = money(request.getKhachThanhToan());
        if (paid.compareTo(order.getTongTienThanhToan()) < 0) {
            throw new IllegalArgumentException("Khách thanh toán chưa đủ tiền");
        }

        for (HoaDonChiTiet item : items) {
            ChiTietSanPham variant = item.getChiTietSanPham();
            validateStock(variant, item.getSoLuong());
            variant.setSoLuongTon(variant.getSoLuongTon() - item.getSoLuong());
            chiTietSanPhamRepo.save(variant);
        }

        PhieuGiamGia voucher = order.getPhieuGiamGia();
        if (voucher != null) {
            validateVoucher(voucher, subtotal(order));
            int used = voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung();
            voucher.setSoLuongDaDung(used + 1);
            voucherRepo.save(voucher);
        }

        order.setTrangThai("Đã thanh toán");
        order.setNgayThanhToan(LocalDateTime.now());
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepo.save(order);
        return toDTO(order, paid);
    }

    @Transactional
    public PosOrderDTO cancel(Integer idHoaDon) {
        HoaDon order = draftOrder(idHoaDon);
        order.setTrangThai("Đã hủy");
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepo.save(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public PosOrderItemDTO findVariantByCode(String code) {
        ChiTietSanPham variant = chiTietSanPhamRepo.findByMaChiTietSanPham(requireText(code, "Vui lòng nhập mã sản phẩm"))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        if (!Boolean.TRUE.equals(variant.getTrangThai()) || !Boolean.TRUE.equals(variant.getSanPham().getTrangThai())) {
            throw new IllegalArgumentException("Sản phẩm đã ngừng bán");
        }
        return toVariantDTO(variant);
    }

    private HoaDon findOrder(Integer id) {
        return hoaDonRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
    }

    private HoaDon draftOrder(Integer id) {
        HoaDon order = findOrder(id);
        if (!"Chờ thanh toán".equals(order.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ thao tác được với đơn chờ thanh toán");
        }
        return order;
    }

    private HoaDonChiTiet findOrderItem(HoaDon order, Integer idHdct) {
        return hoaDonChiTietRepo.findById(idHdct)
                .filter(item -> Objects.equals(item.getHoaDon().getId(), order.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong đơn"));
    }

    private ChiTietSanPham findSellableVariant(Integer idSpct) {
        ChiTietSanPham variant = chiTietSanPhamRepo.findById(idSpct)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm"));
        if (!Boolean.TRUE.equals(variant.getTrangThai())
                || variant.getSanPham() == null
                || !Boolean.TRUE.equals(variant.getSanPham().getTrangThai())) {
            throw new IllegalArgumentException("Sản phẩm đã ngừng bán");
        }
        return variant;
    }

    private void validateStock(ChiTietSanPham variant, int quantity) {
        int stock = variant.getSoLuongTon() == null ? 0 : variant.getSoLuongTon();
        if (stock < quantity) {
            throw new IllegalArgumentException("Sản phẩm " + variant.getMaChiTietSanPham() + " chỉ còn " + stock + " sản phẩm");
        }
    }

    private void recalculate(HoaDon order) {
        BigDecimal subtotal = subtotal(order);
        BigDecimal discount = BigDecimal.ZERO;
        if (order.getPhieuGiamGia() != null) {
            validateVoucher(order.getPhieuGiamGia(), subtotal);
            discount = calculateDiscount(order.getPhieuGiamGia(), subtotal);
        }
        order.setTongTienGoc(subtotal);
        order.setSoTienGiam(discount);
        order.setPhiVanChuyen(BigDecimal.ZERO);
        order.setTongTienThanhToan(subtotal.subtract(discount).max(BigDecimal.ZERO));
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepo.save(order);
    }

    private BigDecimal subtotal(HoaDon order) {
        return hoaDonChiTietRepo.findByHoaDon_Id(order.getId()).stream()
                .map(item -> money(item.getDonGia()).multiply(BigDecimal.valueOf(item.getSoLuong() == null ? 0 : item.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateVoucher(PhieuGiamGia voucher, BigDecimal subtotal) {
        if (!Boolean.TRUE.equals(voucher.getTrangThai())) {
            throw new IllegalArgumentException("Mã giảm giá đã tắt");
        }
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() != null && now.isBefore(voucher.getNgayBatDau())) {
            throw new IllegalArgumentException("Mã giảm giá chưa đến thời gian áp dụng");
        }
        if (voucher.getNgayKetThuc() != null && now.isAfter(voucher.getNgayKetThuc())) {
            throw new IllegalArgumentException("Mã giảm giá đã hết hạn");
        }
        if (voucher.getSoLuong() != null) {
            int used = voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung();
            if (used >= voucher.getSoLuong()) {
                throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng");
            }
        }
        if (voucher.getDieuKienDonHang() != null && subtotal.compareTo(voucher.getDieuKienDonHang()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt điều kiện của mã giảm giá");
        }
    }

    private BigDecimal calculateDiscount(PhieuGiamGia voucher, BigDecimal subtotal) {
        BigDecimal value = money(voucher.getGiaTri());
        if (value.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal discount;
        if (isPercentVoucher(voucher.getLoaiGiam())) {
            discount = subtotal.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getGiaTriToiDa() != null && voucher.getGiaTriToiDa().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(voucher.getGiaTriToiDa());
            }
        } else {
            discount = value;
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    private PosOrderDTO toDTO(HoaDon order, BigDecimal paid) {
        List<PosOrderItemDTO> items = hoaDonChiTietRepo.findByHoaDon_Id(order.getId()).stream()
                .map(this::toItemDTO)
                .toList();
        BigDecimal total = money(order.getTongTienThanhToan());
        BigDecimal safePaid = money(paid);
        return PosOrderDTO.builder()
                .id(order.getId())
                .maHoaDon(order.getMaHoaDon())
                .loaiDon(order.getLoaiDon())
                .trangThai(order.getTrangThai())
                .ngayTao(order.getNgayTao())
                .tenKhachHang(order.getTenKhachHang())
                .soDienThoai(order.getSoDienThoai())
                .diaChiKhachHang(order.getDiaChiKhachHang())
                .idKhachHang(order.getKhachHang() == null ? null : order.getKhachHang().getId())
                .maVoucher(order.getPhieuGiamGia() == null ? null : order.getPhieuGiamGia().getMaPgg())
                .tongTienGoc(money(order.getTongTienGoc()))
                .soTienGiam(money(order.getSoTienGiam()))
                .tongTienThanhToan(total)
                .khachThanhToan(safePaid)
                .tienThua(safePaid.subtract(total).max(BigDecimal.ZERO))
                .items(items)
                .build();
    }

    private PosOrderItemDTO toItemDTO(HoaDonChiTiet item) {
        ChiTietSanPham variant = item.getChiTietSanPham();
        SanPham product = variant.getSanPham();
        String image = firstImage(product);
        return PosOrderItemDTO.builder()
                .idHdct(item.getId())
                .idSpct(variant.getIdSpct())
                .maSp(product.getMaSp())
                .tenSanPham(product.getTenSp())
                .maChiTietSanPham(variant.getMaChiTietSanPham())
                .hinhAnh(image)
                .imageUrl(imageUrl(image))
                .mauSac(variant.getMauSac() == null ? null : variant.getMauSac().getTenMauSac())
                .kichCo(variant.getKichCo() == null ? null : variant.getKichCo().getTenKichCo())
                .donGia(money(item.getDonGia()))
                .soLuong(item.getSoLuong())
                .soLuongTon(variant.getSoLuongTon())
                .thanhTien(money(item.getDonGia()).multiply(BigDecimal.valueOf(item.getSoLuong() == null ? 0 : item.getSoLuong())))
                .build();
    }

    private PosOrderItemDTO toVariantDTO(ChiTietSanPham variant) {
        SanPham product = variant.getSanPham();
        String image = firstImage(product);
        return PosOrderItemDTO.builder()
                .idSpct(variant.getIdSpct())
                .maSp(product.getMaSp())
                .tenSanPham(product.getTenSp())
                .maChiTietSanPham(variant.getMaChiTietSanPham())
                .hinhAnh(image)
                .imageUrl(imageUrl(image))
                .mauSac(variant.getMauSac() == null ? null : variant.getMauSac().getTenMauSac())
                .kichCo(variant.getKichCo() == null ? null : variant.getKichCo().getTenKichCo())
                .donGia(money(variant.getDonGia()))
                .soLuongTon(variant.getSoLuongTon())
                .build();
    }

    private String firstImage(SanPham product) {
        String mainImage = trimToNull(product.getHinhAnh());
        if (mainImage != null) return mainImage;
        return hinhAnhRepo.findByIdSanPham(product.getIdSp()).stream()
                .map(HinhAnhSanPham::getUrlAnh)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String imageUrl(String value) {
        String image = trimToNull(value);
        if (image == null) return null;
        String lower = image.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || image.startsWith("/")) {
            return image;
        }
        return "/uploads/" + image;
    }

    private boolean isPercentVoucher(String type) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        return normalized.contains("phan") || normalized.contains("percent") || normalized.contains("%");
    }

    private String generateOrderCode() {
        int suffix = ThreadLocalRandom.current().nextInt(100, 1000);
        return "HD" + ORDER_CODE_FORMAT.format(LocalDateTime.now()) + suffix;
    }

    private String firstNonBlank(String first, String second) {
        String value = trimToNull(first);
        return value == null ? trimToNull(second) : value;
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) throw new IllegalArgumentException(message);
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
