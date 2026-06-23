package com.example.sp.service.shop;

import com.example.sp.dto.shop.ShopLookupDTO;
import com.example.sp.dto.shop.ShopOrderItemRequest;
import com.example.sp.dto.shop.ShopOrderRequest;
import com.example.sp.dto.shop.ShopOrderResponse;
import com.example.sp.dto.shop.ShopProductDTO;
import com.example.sp.dto.shop.ShopVariantDTO;
import com.example.sp.dto.shop.ShopVoucherDTO;
import com.example.sp.model.ChatLieu;
import com.example.sp.model.ChiTietSanPham;
import com.example.sp.model.HinhAnhSanPham;
import com.example.sp.model.KichCo;
import com.example.sp.model.KieuDang;
import com.example.sp.model.LoaiAo;
import com.example.sp.model.MauSac;
import com.example.sp.model.PhongCachMac;
import com.example.sp.model.SanPham;
import com.example.sp.model.XuatXu;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ShopService {

    private static final BigDecimal DEFAULT_SHIP_FEE = new BigDecimal("30000");
    private static final DateTimeFormatter ORDER_CODE_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final ChiTietSanPhamRepository chiTietRepo;
    private final HinhAnhSanPhamRepository hinhAnhRepo;
    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepo;
    private final PhieuGiamGiaRepository voucherRepo;
    private final KhachHangRepository khachHangRepo;

    @Transactional(readOnly = true)
    public Page<ShopProductDTO> getProducts(
            String keyword,
            String loaiAo,
            String kichCo,
            String mauSac,
            BigDecimal giaMin,
            BigDecimal giaMax,
            String sort,
            int page,
            int size
    ) {
        Set<String> loaiAoFilter = splitFilter(loaiAo);
        Set<String> kichCoFilter = splitFilter(kichCo);
        Set<String> mauSacFilter = splitFilter(mauSac);
        String normalizedKeyword = normalize(keyword);

        Map<Integer, List<ChiTietSanPham>> grouped = new LinkedHashMap<>();
        for (ChiTietSanPham variant : chiTietRepo.findActiveSellableVariants()) {
            if (!matchesKeyword(variant, normalizedKeyword)) continue;
            if (!matchesLookup(loaiAoFilter, tenLoaiAo(variant.getLoaiAo()))) continue;
            if (!matchesLookup(kichCoFilter, tenKichCo(variant.getKichCo()))) continue;
            if (!matchesLookup(mauSacFilter, tenMauSac(variant.getMauSac()))) continue;
            if (!matchesPrice(variant.getDonGia(), giaMin, giaMax)) continue;

            Integer idSp = variant.getSanPham().getIdSp();
            grouped.computeIfAbsent(idSp, ignored -> new ArrayList<>()).add(variant);
        }

        List<ShopProductDTO> products = grouped.values().stream()
                .map(this::toProductDTO)
                .sorted(productComparator(sort))
                .toList();

        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int from = Math.min(safePage * safeSize, products.size());
        int to = Math.min(from + safeSize, products.size());
        Pageable pageable = PageRequest.of(safePage, safeSize);

        return new PageImpl<>(products.subList(from, to), pageable, products.size());
    }

    @Transactional(readOnly = true)
    public ShopProductDTO getProduct(Integer idSp) {
        List<ChiTietSanPham> variants = chiTietRepo.findActiveSellableVariantsByProductId(idSp);
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã hết hàng");
        }
        return toProductDTO(variants);
    }

    @Transactional(readOnly = true)
    public List<ShopVariantDTO> getVariants(Integer idSp) {
        return chiTietRepo.findActiveSellableVariantsByProductId(idSp).stream()
                .map(this::toVariantDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShopVoucherDTO getVoucher(String code, BigDecimal subtotal) {
        PhieuGiamGia voucher = findVoucherByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại"));
        BigDecimal safeSubtotal = money(subtotal);
        validateVoucher(voucher, safeSubtotal);
        return toVoucherDTO(voucher, calculateDiscount(voucher, safeSubtotal));
    }

    @Transactional
    public ShopOrderResponse createOrder(ShopOrderRequest request, Integer customerId) {
        KhachHang customer = resolveCustomer(customerId);
        String name = requireText(firstNonBlank(request.getTenKhachHang(), customer == null ? null : customer.getTenKhachHang()), "Vui lòng nhập họ tên");
        String phone = normalizePhone(firstNonBlank(request.getSoDienThoai(), customer == null ? null : customer.getSoDienThoai()));
        String address = requireText(firstNonBlank(request.getDiaChiKhachHang(), customer == null ? null : customer.getDiaChi()), "Vui lòng nhập địa chỉ giao hàng");
        String paymentMethod = normalizePaymentMethod(request.getPhuongThucThanhToan());
        String note = buildOrderNote(request.getGhiChu(), paymentMethod);

        Map<Integer, Integer> quantityByVariant = mergeItems(request.getItems());
        List<OrderLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Integer, Integer> entry : quantityByVariant.entrySet()) {
            ChiTietSanPham variant = chiTietRepo.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm id=" + entry.getKey()));
            int quantity = entry.getValue();
            validateSellableVariant(variant, quantity);

            BigDecimal price = money(variant.getDonGia());
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(lineTotal);
            lines.add(new OrderLine(variant, quantity, price, lineTotal));
        }

        PhieuGiamGia voucher = resolveVoucher(request.getIdVoucher(), request.getMaVoucher()).orElse(null);
        BigDecimal discount = BigDecimal.ZERO;
        if (voucher != null) {
            validateVoucher(voucher, subtotal);
            discount = calculateDiscount(voucher, subtotal);
        }

        BigDecimal shipFee = request.getPhiVanChuyen() == null
                ? estimateShippingFee(address, subtotal)
                : request.getPhiVanChuyen().max(BigDecimal.ZERO);
        BigDecimal total = subtotal.subtract(discount).add(shipFee).max(BigDecimal.ZERO);

        HoaDon order = HoaDon.builder()
                .maHoaDon(generateOrderCode())
                .loaiDon("Trực tuyến")
                .phiVanChuyen(shipFee)
                .tongTienGoc(subtotal)
                .soTienGiam(discount)
                .tongTienThanhToan(total)
                .tenKhachHang(name)
                .diaChiKhachHang(address)
                .soDienThoai(phone)
                .ghiChu(note)
                .trangThai("ONLINE".equals(paymentMethod) ? "Chờ thanh toán online" : "Chờ xác nhận")
                .ngayTao(LocalDateTime.now())
                .ngayCapNhat(LocalDateTime.now())
                .khachHang(customer)
                .phieuGiamGia(voucher)
                .build();
        order = hoaDonRepo.save(order);

        for (OrderLine line : lines) {
            ChiTietSanPham variant = line.variant();
            variant.setSoLuongTon(variant.getSoLuongTon() - line.quantity());
            chiTietRepo.save(variant);

            HoaDonChiTiet detail = HoaDonChiTiet.builder()
                    .hoaDon(order)
                    .chiTietSanPham(variant)
                    .donGia(line.price())
                    .soLuong(line.quantity())
                    .thanhTien(line.lineTotal())
                    .build();
            hoaDonChiTietRepo.save(detail);
        }

        if (voucher != null) {
            int used = voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung();
            voucher.setSoLuongDaDung(used + 1);
            voucherRepo.save(voucher);
        }

        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public BigDecimal estimateShippingFee(String address, BigDecimal subtotal) {
        BigDecimal safeSubtotal = money(subtotal);
        if (safeSubtotal.compareTo(new BigDecimal("500000")) >= 0) {
            return BigDecimal.ZERO;
        }

        String normalizedAddress = normalize(address);
        if (normalizedAddress == null) {
            return DEFAULT_SHIP_FEE;
        }
        if (containsAny(normalizedAddress, "hà nội", "ha noi", "hồ chí minh", "ho chi minh", "tp hcm", "hcm", "đà nẵng", "da nang")) {
            return new BigDecimal("25000");
        }
        if (containsAny(normalizedAddress, "hải phòng", "hai phong", "cần thơ", "can tho", "bình dương", "binh duong", "đồng nai", "dong nai")) {
            return new BigDecimal("30000");
        }
        return new BigDecimal("38000");
    }

    private KhachHang resolveCustomer(Integer customerId) {
        if (customerId == null) return null;
        return khachHangRepo.findById(customerId)
                .filter(customer -> Boolean.TRUE.equals(customer.getTrangThai()))
                .orElse(null);
    }

    private ShopProductDTO toProductDTO(List<ChiTietSanPham> variants) {
        SanPham product = variants.get(0).getSanPham();
        BigDecimal minPrice = variants.stream()
                .map(ChiTietSanPham::getDonGia)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = variants.stream()
                .map(ChiTietSanPham::getDonGia)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        int stock = variants.stream()
                .map(ChiTietSanPham::getSoLuongTon)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        List<ShopLookupDTO> loaiAos = distinct(variants.stream().map(v -> lookup(v.getLoaiAo())).toList());
        List<ShopLookupDTO> kichCos = distinct(variants.stream().map(v -> lookup(v.getKichCo())).toList());
        List<ShopLookupDTO> mauSacs = distinct(variants.stream().map(v -> lookup(v.getMauSac())).toList());
        List<ShopLookupDTO> phongCachs = distinct(variants.stream().map(v -> lookup(v.getPhongCachMac())).toList());
        List<ShopLookupDTO> kieuDangs = distinct(variants.stream().map(v -> lookup(v.getKieuDang())).toList());

        String image = firstImage(product);
        return ShopProductDTO.builder()
                .idSp(product.getIdSp())
                .maSp(product.getMaSp())
                .tenSp(product.getTenSp())
                .moTa(product.getMoTa())
                .hinhAnh(image)
                .imageUrl(imageUrl(image))
                .trangThai(product.getTrangThai())
                .ngayTao(product.getNgayTao())
                .giaBanMin(minPrice)   // thực tế là đơn giá thấp nhất
                .giaBanMax(maxPrice)   // đơn giá cao nhất
                .tongTon(stock)
                .chatLieu(lookup(product.getChatLieu()))
                .xuatXu(lookup(product.getXuatXu()))
                .loaiAo(loaiAos.isEmpty() ? null : loaiAos.get(0).getTen())
                .loaiAos(loaiAos)
                .kichCos(kichCos)
                .mauSacs(mauSacs)
                .phongCachMacs(phongCachs)
                .kieuDangs(kieuDangs)
                .build();
    }

    private ShopVariantDTO toVariantDTO(ChiTietSanPham variant) {
        SanPham product = variant.getSanPham();
        String image = firstImage(product);
        return ShopVariantDTO.builder()
                .idSpct(variant.getIdSpct())
                .idSanPham(variant.getIdSanPham())
                .maChiTietSanPham(variant.getMaChiTietSanPham())
                .tenSanPham(product.getTenSp())
                .hinhAnh(image)
                .imageUrl(imageUrl(image))
                .soLuongTon(variant.getSoLuongTon())
                .donGia(variant.getDonGia())   // thay giaBan bằng donGia
                .kichCo(lookup(variant.getKichCo()))
                .mauSac(lookup(variant.getMauSac()))
                .loaiAo(lookup(variant.getLoaiAo()))
                .phongCachMac(lookup(variant.getPhongCachMac()))
                .kieuDang(lookup(variant.getKieuDang()))
                .build();
    }

    private ShopOrderResponse toOrderResponse(HoaDon order) {
        return ShopOrderResponse.builder()
                .id(order.getId())
                .maHoaDon(order.getMaHoaDon())
                .trangThai(order.getTrangThai())
                .phuongThucThanhToan(readPaymentMethod(order.getGhiChu()))
                .invoiceUrl("/api/shop/orders/" + order.getId() + "/invoice")
                .tongTienGoc(order.getTongTienGoc())
                .soTienGiam(order.getSoTienGiam())
                .phiVanChuyen(order.getPhiVanChuyen())
                .tongTienThanhToan(order.getTongTienThanhToan())
                .build();
    }

    private ShopVoucherDTO toVoucherDTO(PhieuGiamGia voucher, BigDecimal discount) {
        return ShopVoucherDTO.builder()
                .id(voucher.getId())
                .maPgg(voucher.getMaPgg())
                .tenPgg(voucher.getTenPgg())
                .loaiGiam(voucher.getLoaiGiam())
                .giaTri(voucher.getGiaTri())
                .giaTriToiDa(voucher.getGiaTriToiDa())
                .dieuKienDonHang(voucher.getDieuKienDonHang())
                .soTienGiam(discount)
                .build();
    }

    private void validateSellableVariant(ChiTietSanPham variant, int quantity) {
        if (!Boolean.TRUE.equals(variant.getTrangThai())
                || variant.getSanPham() == null
                || !Boolean.TRUE.equals(variant.getSanPham().getTrangThai())) {
            throw new IllegalArgumentException("Sản phẩm đã ngừng bán");
        }
        int stock = variant.getSoLuongTon() == null ? 0 : variant.getSoLuongTon();
        if (stock < quantity) {
            throw new IllegalArgumentException("Sản phẩm " + variant.getMaChiTietSanPham() + " chỉ còn " + stock + " sản phẩm");
        }
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
        if (voucher.getDieuKienDonHang() != null
                && subtotal.compareTo(voucher.getDieuKienDonHang()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt điều kiện của mã giảm giá");
        }
    }

    private BigDecimal calculateDiscount(PhieuGiamGia voucher, BigDecimal subtotal) {
        BigDecimal value = money(voucher.getGiaTri());
        if (value.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal discount;
        if (isPercentVoucher(voucher.getLoaiGiam())) {
            discount = subtotal.multiply(value)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getGiaTriToiDa() != null && voucher.getGiaTriToiDa().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(voucher.getGiaTriToiDa());
            }
        } else {
            discount = value;
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    private Optional<PhieuGiamGia> resolveVoucher(Integer idVoucher, String maVoucher) {
        if (idVoucher != null) return voucherRepo.findById(idVoucher);
        String code = trimToNull(maVoucher);
        return code == null ? Optional.empty() : findVoucherByCode(code);
    }

    private Optional<PhieuGiamGia> findVoucherByCode(String code) {
        String normalized = trimToNull(code);
        return normalized == null ? Optional.empty() : voucherRepo.findFirstByMaPggIgnoreCase(normalized);
    }

    private Map<Integer, Integer> mergeItems(List<ShopOrderItemRequest> items) {
        Map<Integer, Integer> merged = new LinkedHashMap<>();
        for (ShopOrderItemRequest item : items) {
            if (item.getIdSpct() == null || item.getSoLuong() == null || item.getSoLuong() < 1) {
                throw new IllegalArgumentException("Dữ liệu giỏ hàng không hợp lệ");
            }
            merged.merge(item.getIdSpct(), item.getSoLuong(), Integer::sum);
        }
        return merged;
    }

    private boolean matchesKeyword(ChiTietSanPham variant, String keyword) {
        if (keyword == null) return true;
        SanPham product = variant.getSanPham();
        String text = String.join(" ",
                safe(product.getMaSp()),
                safe(product.getTenSp()),
                safe(product.getMoTa()),
                safe(tenChatLieu(product.getChatLieu())),
                safe(tenLoaiAo(variant.getLoaiAo())),
                safe(tenMauSac(variant.getMauSac())),
                safe(tenKichCo(variant.getKichCo()))
        ).toLowerCase(Locale.ROOT);
        return text.contains(keyword);
    }

    private boolean matchesLookup(Set<String> filters, String value) {
        return filters.isEmpty() || filters.contains(normalize(value));
    }

    private boolean matchesPrice(BigDecimal price, BigDecimal min, BigDecimal max) {
        BigDecimal safePrice = money(price);
        if (min != null && safePrice.compareTo(min) < 0) return false;
        return max == null || safePrice.compareTo(max) <= 0;
    }

    private Comparator<ShopProductDTO> productComparator(String sort) {
        String normalized = normalize(sort);
        Comparator<ShopProductDTO> newest = Comparator
                .comparing(ShopProductDTO::getNgayTao, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();
        if ("price_asc".equals(normalized)) {
            return Comparator.comparing(ShopProductDTO::getGiaBanMin, Comparator.nullsLast(BigDecimal::compareTo));
        }
        if ("price_desc".equals(normalized)) {
            return Comparator.comparing(ShopProductDTO::getGiaBanMin, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
        }
        return newest;
    }

    private Set<String> splitFilter(String value) {
        Set<String> result = new LinkedHashSet<>();
        String normalized = trimToNull(value);
        if (normalized == null) return result;
        for (String item : normalized.split(",")) {
            String token = normalize(item);
            if (token != null) result.add(token);
        }
        return result;
    }

    private List<ShopLookupDTO> distinct(Collection<ShopLookupDTO> values) {
        Map<String, ShopLookupDTO> result = new LinkedHashMap<>();
        for (ShopLookupDTO value : values) {
            if (value == null || trimToNull(value.getTen()) == null) continue;
            String key = value.getId() == null ? "ten:" + normalize(value.getTen()) : "id:" + value.getId();
            result.putIfAbsent(key, value);
        }
        return new ArrayList<>(result.values());
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

    private ShopLookupDTO lookup(ChatLieu value) {
        return value == null ? null : ShopLookupDTO.builder().id(value.getIdChatLieu()).ten(value.getTenChatLieu()).build();
    }

    private ShopLookupDTO lookup(XuatXu value) {
        return value == null ? null : ShopLookupDTO.builder().id(value.getIdXuatXu()).ten(value.getTenXuatXu()).build();
    }

    private ShopLookupDTO lookup(KichCo value) {
        return value == null ? null : ShopLookupDTO.builder().id(value.getIdKichCo()).ten(value.getTenKichCo()).build();
    }

    private ShopLookupDTO lookup(MauSac value) {
        return value == null ? null : ShopLookupDTO.builder().id(value.getIdMauSac()).ten(value.getTenMauSac()).build();
    }

    private ShopLookupDTO lookup(LoaiAo value) {
        return value == null ? null : ShopLookupDTO.builder().id(value.getIdLoaiAo()).ten(value.getTenLoai()).build();
    }

    private ShopLookupDTO lookup(PhongCachMac value) {
        return value == null ? null : ShopLookupDTO.builder().id(value.getIdPhongCachMac()).ten(value.getTenPhongCach()).build();
    }

    private ShopLookupDTO lookup(KieuDang value) {
        return value == null ? null : ShopLookupDTO.builder().id(value.getIdKieuDang()).ten(value.getTenKieuDang()).build();
    }

    private String tenChatLieu(ChatLieu value) {
        return value == null ? null : value.getTenChatLieu();
    }

    private String tenKichCo(KichCo value) {
        return value == null ? null : value.getTenKichCo();
    }

    private String tenMauSac(MauSac value) {
        return value == null ? null : value.getTenMauSac();
    }

    private String tenLoaiAo(LoaiAo value) {
        return value == null ? null : value.getTenLoai();
    }

    private boolean isPercentVoucher(String type) {
        String normalized = normalize(type);
        return normalized != null
                && (normalized.contains("phan") || normalized.contains("percent") || normalized.contains("%"));
    }

    private String generateOrderCode() {
        int suffix = ThreadLocalRandom.current().nextInt(100, 1000);
        return "HD" + ORDER_CODE_FORMAT.format(LocalDateTime.now()) + suffix;
    }

    private String normalizePhone(String value) {
        String phone = requireText(value, "Vui lòng nhập số điện thoại").replaceAll("\\D", "");
        if (!phone.matches("^(03|05|07|08|09)\\d{8}$")) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam");
        }
        return phone;
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

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record OrderLine(ChiTietSanPham variant, int quantity, BigDecimal price, BigDecimal lineTotal) {
    }

    private String normalizePaymentMethod(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return "COD";
        String upper = normalized.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "ONLINE", "VNPAY", "MOMO", "ZALOPAY", "BANKING" -> upper;
            default -> "COD";
        };
    }

    private String buildOrderNote(String note, String paymentMethod) {
        String trimmedNote = trimToNull(note);
        String paymentLine = "Phương thức thanh toán: " + paymentMethod;
        return trimmedNote == null ? paymentLine : trimmedNote + "\n" + paymentLine;
    }

    private String readPaymentMethod(String note) {
        if (note == null) return "COD";
        for (String line : note.split("\n")) {
            if (line.startsWith("Phương thức thanh toán: ")) {
                return line.substring("Phương thức thanh toán: ".length()).trim();
            }
        }
        return "COD";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}