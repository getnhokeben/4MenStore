package com.example.sp.service.cuahang;

import com.example.sp.dto.cuahang.ShopLookupDTO;
import com.example.sp.dto.cuahang.ShopOrderHistoryDTO;
import com.example.sp.dto.cuahang.ShopOrderHistoryItemDTO;
import com.example.sp.dto.cuahang.ShopOrderItemRequest;
import com.example.sp.dto.cuahang.ShopOrderRequest;
import com.example.sp.dto.cuahang.ShopOrderResponse;
import com.example.sp.dto.cuahang.ShopProductDTO;
import com.example.sp.dto.cuahang.ShopVariantDTO;
import com.example.sp.dto.cuahang.ShopVoucherDTO;
import com.example.sp.model.sanpham.ChatLieu;
import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.model.sanpham.HinhAnhSanPham;
import com.example.sp.model.sanpham.KichCo;
import com.example.sp.model.sanpham.KieuDang;
import com.example.sp.model.sanpham.LoaiAo;
import com.example.sp.model.sanpham.MauSac;
import com.example.sp.model.sanpham.PhongCachMac;
import com.example.sp.model.sanpham.SanPham;
import com.example.sp.model.sanpham.XuatXu;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.HoaDonChiTiet;
import com.example.sp.model.khuyenmai.DotGiamGia;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import com.example.sp.repository.khuyenmai.ChiTietDotGiamGiaRepository;
import com.example.sp.repository.sanpham.ChiTietSanPhamRepository;
import com.example.sp.repository.sanpham.HinhAnhSanPhamRepository;
import com.example.sp.repository.hoadon.HoaDonChiTietRepository;
import com.example.sp.repository.hoadon.HoaDonRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.khuyenmai.PhieuGiamGiaRepository;
import com.example.sp.service.tienich.GeneratedCodeUtil;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.service.hoadon.OrderStatusMailService;
import com.example.sp.service.tonkho.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private static final BigDecimal DEFAULT_SHIP_FEE = new BigDecimal("30000");
    private final ChiTietSanPhamRepository chiTietRepo;
    private final HinhAnhSanPhamRepository hinhAnhRepo;
    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepo;
    private final PhieuGiamGiaRepository voucherRepo;
    private final KhachHangRepository khachHangRepo;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepo;
    private final ShopOrderMailService shopOrderMailService;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public Page<ShopProductDTO> getProducts(
            String keyword,
            String loaiAo,
            String kichCo,
            String mauSac,
            String chatLieu,
            String xuatXu,
            String phongCachMac,
            String kieuDang,
            BigDecimal giaMin,
            BigDecimal giaMax,
            String sort,
            int page,
            int size
    ) {
        Set<String> loaiAoFilter = splitFilter(loaiAo);
        Set<String> kichCoFilter = splitFilter(kichCo);
        Set<String> mauSacFilter = splitFilter(mauSac);
        Set<String> chatLieuFilter = splitFilter(chatLieu);
        Set<String> xuatXuFilter = splitFilter(xuatXu);
        Set<String> phongCachFilter = splitFilter(phongCachMac);
        Set<String> kieuDangFilter = splitFilter(kieuDang);
        String normalizedKeyword = normalize(keyword);

        Map<Integer, List<ChiTietSanPham>> grouped = new LinkedHashMap<>();
        for (ChiTietSanPham variant : chiTietRepo.findActiveSellableVariants()) {
            if (!matchesKeyword(variant, normalizedKeyword)) continue;
            if (!matchesLookup(loaiAoFilter, tenLoaiAo(variant.getLoaiAo()))) continue;
            if (!matchesLookup(kichCoFilter, tenKichCo(variant.getKichCo()))) continue;
            if (!matchesLookup(mauSacFilter, tenMauSac(variant.getMauSac()))) continue;
            if (!matchesLookup(chatLieuFilter, tenChatLieu(variant.getSanPham().getChatLieu()))) continue;
            if (!matchesLookup(xuatXuFilter, tenXuatXu(variant.getSanPham().getXuatXu()))) continue;
            if (!matchesLookup(phongCachFilter, tenPhongCach(variant.getPhongCachMac()))) continue;
            if (!matchesLookup(kieuDangFilter, tenKieuDang(variant.getKieuDang()))) continue;
            if (!matchesPrice(salePrice(variant), giaMin, giaMax)) continue;

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
        BigDecimal safeSubtotal = MoneyRoundingUtil.roundNonNegative(subtotal);
        validateVoucher(voucher, safeSubtotal);
        return toVoucherDTO(voucher, safeSubtotal, voucher.getMaPgg());
    }

    @Transactional(readOnly = true)
    public List<ShopVoucherDTO> getVouchers(BigDecimal subtotal) {
        BigDecimal safeSubtotal = MoneyRoundingUtil.roundNonNegative(subtotal);
        return voucherRepo.findByTrangThaiTrue().stream()
                .filter(this::isVoucherInSaleWindow)
                .map(voucher -> toVoucherDTO(voucher, safeSubtotal, null))
                .sorted((left, right) -> {
                    int usable = Boolean.compare(Boolean.TRUE.equals(right.getApplicable()), Boolean.TRUE.equals(left.getApplicable()));
                    if (usable != 0) return usable;
                    int discount = money(right.getSoTienGiam()).compareTo(money(left.getSoTienGiam()));
                    if (discount != 0) return discount;
                    return money(left.getCanMuaThem()).compareTo(money(right.getCanMuaThem()));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShopOrderHistoryDTO> getOrderHistory(Integer customerId) {
        if (customerId == null) {
            return List.of();
        }
        return hoaDonRepo.findByKhachHang_IdOrderByNgayTaoDesc(customerId).stream()
                .map(this::toOrderHistoryDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShopOrderHistoryDTO lookupOrder(String maHoaDon) {
        String code = requireText(maHoaDon, "Vui long nhap ma don hang");
        return hoaDonRepo.findFirstByMaHoaDonIgnoreCase(code)
                .map(this::toOrderHistoryDTO)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don hang phu hop"));
    }

    @Transactional
    public ShopOrderResponse createOrder(ShopOrderRequest request, Integer customerId) {
        KhachHang customer = resolveCustomer(customerId);
        String name = requireText(firstNonBlank(request.getTenKhachHang(), customer == null ? null : customer.getTenKhachHang()), "Vui lòng nhập họ tên");
        String phone = normalizePhone(firstNonBlank(request.getSoDienThoai(), customer == null ? null : customer.getSoDienThoai()));
        String email = requireEmail(firstNonBlank(request.getEmail(), customer == null ? null : customer.getEmail()));
        String address = requireText(firstNonBlank(request.getDiaChiKhachHang(), customer == null ? null : customer.getDiaChi()), "Vui lòng nhập địa chỉ giao hàng");
        String paymentMethod = normalizePaymentMethod(request.getPhuongThucThanhToan());
        String note = buildOrderNote(request.getGhiChu(), paymentMethod, email);

        Map<Integer, Integer> quantityByVariant = mergeItems(request.getItems());
        List<OrderLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean deductStockImmediately = false;
        boolean reserveUntilGatewayCallback = isGatewayPayment(paymentMethod);

        for (Map.Entry<Integer, Integer> entry : quantityByVariant.entrySet()) {
            int quantity = entry.getValue();
            ChiTietSanPham variant;
            if (deductStockImmediately) {
                variant = inventoryService.deductOnlineStock(entry.getKey(), quantity);
            } else if (reserveUntilGatewayCallback) {
                variant = inventoryService.reserveOnline(entry.getKey(), quantity);
            } else {
                // COD is validated now, then deducted only when admin confirms.
                variant = inventoryService.validateOnlineAvailability(entry.getKey(), quantity);
            }

            BigDecimal price = MoneyRoundingUtil.roundNonNegative(salePrice(variant));
            BigDecimal lineTotal = MoneyRoundingUtil.roundNonNegative(price.multiply(BigDecimal.valueOf(quantity)));
            subtotal = MoneyRoundingUtil.roundNonNegative(subtotal.add(lineTotal));
            lines.add(new OrderLine(variant, quantity, price, lineTotal));
        }

        PhieuGiamGia voucher = resolveVoucher(request.getIdVoucher(), request.getMaVoucher()).orElse(null);
        BigDecimal discount = BigDecimal.ZERO;
        if (voucher != null) {
            validateVoucher(voucher, subtotal);
            discount = calculateDiscount(voucher, subtotal);
        }

        BigDecimal shipFee = MoneyRoundingUtil.roundNonNegative(request.getPhiVanChuyen() == null
                ? estimateShippingFee(address, subtotal)
                : request.getPhiVanChuyen());
        BigDecimal total = MoneyRoundingUtil.roundNonNegative(subtotal.subtract(discount).add(shipFee));
        String initialStatus = deductStockImmediately
                ? "Đã xác nhận"
                : "COD".equals(paymentMethod) ? "Chờ xác nhận" : "Chờ thanh toán online";

        HoaDon order = HoaDon.builder()
                .maHoaDon(generateOrderCode(name))
                .loaiDon("Trực tuyến")
                .hinhThucNhanHang("Giao hàng")
                .phiVanChuyen(shipFee)
                .tongTienGoc(subtotal)
                .soTienGiam(discount)
                .tongTienThanhToan(total)
                .tenKhachHang(name)
                .diaChiKhachHang(address)
                .soDienThoai(phone)
                .ghiChu(note)
                .trangThai(initialStatus)
                .daGiuTon(reserveUntilGatewayCallback)
                .daTruTon(deductStockImmediately)
                .ngayTao(LocalDateTime.now())
                .ngayCapNhat(LocalDateTime.now())
                .khachHang(customer)
                .phieuGiamGia(voucher)
                .build();
        order = hoaDonRepo.save(order);

        for (OrderLine line : lines) {
            ChiTietSanPham variant = line.variant();

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

        ShopOrderHistoryDTO orderMailData = toOrderHistoryDTO(order);
        try {
            shopOrderMailService.sendOrderConfirmation(email, orderMailData);
        } catch (RuntimeException ex) {
            log.warn("Khong gui duoc mail xac nhan don hang {}", order.getMaHoaDon(), ex);
        }

        return toOrderResponse(order, email);
    }

    @Transactional(readOnly = true)
    public BigDecimal estimateShippingFee(String address, BigDecimal subtotal) {
        BigDecimal safeSubtotal = MoneyRoundingUtil.roundNonNegative(subtotal);
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
        PriceInfo displayPrice = variants.stream()
                .map(this::priceInfo)
                .min(Comparator
                        .comparing(PriceInfo::current)
                        .thenComparing(PriceInfo::original))
                .orElse(new PriceInfo(BigDecimal.ZERO, BigDecimal.ZERO, Optional.empty()));
        BigDecimal maxPrice = variants.stream()
                .map(this::salePrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        int stock = variants.stream()
                .map(ChiTietSanPham::getSoLuongKhaDung)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        Optional<DotGiamGia> activeCampaign = displayPrice.campaign()
                .filter(campaign -> displayPrice.current().compareTo(displayPrice.original()) < 0);

        List<ShopLookupDTO> loaiAos = distinct(variants.stream().map(v -> lookup(v.getLoaiAo())).toList());
        List<ShopLookupDTO> kichCos = distinct(variants.stream().map(v -> lookup(v.getKichCo())).toList());
        List<ShopLookupDTO> mauSacs = distinct(variants.stream().map(v -> lookup(v.getMauSac())).toList());
        List<ShopLookupDTO> phongCachs = distinct(variants.stream().map(v -> lookup(v.getPhongCachMac())).toList());
        List<ShopLookupDTO> kieuDangs = distinct(variants.stream().map(v -> lookup(v.getKieuDang())).toList());

        String image = firstImage(variants.get(0));
        return ShopProductDTO.builder()
                .idSp(product.getIdSp())
                .maSp(product.getMaSp())
                .tenSp(product.getTenSp())
                .moTa(product.getMoTa())
                .hinhAnh(image)
                .imageUrl(imageUrl(image))
                .trangThai(product.getTrangThai())
                .ngayTao(product.getNgayTao())
                .giaGocMin(displayPrice.original())
                .giaBanMin(displayPrice.current())   // thực tế là đơn giá thấp nhất
                .giaBanMax(maxPrice)   // đơn giá cao nhất
                .dangGiamGia(activeCampaign.isPresent())
                .tenDotGiamGia(activeCampaign.map(DotGiamGia::getTenDotGiamGia).orElse(null))
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

    private PriceInfo priceInfo(ChiTietSanPham variant) {
        BigDecimal original = MoneyRoundingUtil.roundNonNegative(variant.getDonGia());
        Optional<DotGiamGia> campaign = activeCampaign(variant);
        BigDecimal current = campaign
                .map(active -> campaignPrice(original, active))
                .orElse(original);
        return new PriceInfo(original, current, campaign);
    }

    private record PriceInfo(BigDecimal original, BigDecimal current, Optional<DotGiamGia> campaign) {
    }

    private ShopVariantDTO toVariantDTO(ChiTietSanPham variant) {
        SanPham product = variant.getSanPham();
        String image = firstImage(variant);
        BigDecimal originalPrice = MoneyRoundingUtil.roundNonNegative(variant.getDonGia());
        Optional<DotGiamGia> activeCampaign = activeCampaign(variant);
        BigDecimal currentPrice = activeCampaign
                .map(campaign -> campaignPrice(originalPrice, campaign))
                .orElse(originalPrice);
        return ShopVariantDTO.builder()
                .idSpct(variant.getIdSpct())
                .idSanPham(variant.getIdSanPham())
                .maChiTietSanPham(variant.getMaChiTietSanPham())
                .tenSanPham(product.getTenSp())
                .hinhAnh(image)
                .imageUrl(imageUrl(image))
                .soLuongTon(variant.getSoLuongKhaDung())
                .giaGoc(originalPrice)
                .donGia(currentPrice)   // giá hiện tại, đã áp đợt giảm giá nếu có
                .dangGiamGia(activeCampaign.isPresent() && currentPrice.compareTo(originalPrice) < 0)
                .tenDotGiamGia(activeCampaign.map(DotGiamGia::getTenDotGiamGia).orElse(null))
                .kichCo(lookup(variant.getKichCo()))
                .mauSac(lookup(variant.getMauSac()))
                .loaiAo(lookup(variant.getLoaiAo()))
                .phongCachMac(lookup(variant.getPhongCachMac()))
                .kieuDang(lookup(variant.getKieuDang()))
                .build();
    }

    private ShopOrderResponse toOrderResponse(HoaDon order, String email) {
        return ShopOrderResponse.builder()
                .id(order.getId())
                .maHoaDon(order.getMaHoaDon())
                .trangThai(order.getTrangThai())
                .email(email)
                .phuongThucThanhToan(readPaymentMethod(order.getGhiChu()))
                .invoiceUrl("/api/shop/orders/" + order.getId() + "/invoice")
                .tongTienGoc(MoneyRoundingUtil.roundNonNegative(order.getTongTienGoc()))
                .soTienGiam(MoneyRoundingUtil.roundNonNegative(order.getSoTienGiam()))
                .phiVanChuyen(MoneyRoundingUtil.roundNonNegative(order.getPhiVanChuyen()))
                .tongTienThanhToan(MoneyRoundingUtil.roundNonNegative(order.getTongTienThanhToan()))
                .build();
    }

    private ShopOrderHistoryDTO toOrderHistoryDTO(HoaDon order) {
        List<ShopOrderHistoryItemDTO> items = hoaDonChiTietRepo.findByHoaDon_Id(order.getId()).stream()
                .map(this::toOrderHistoryItemDTO)
                .toList();
        PhieuGiamGia voucher = order.getPhieuGiamGia();
        return ShopOrderHistoryDTO.builder()
                .id(order.getId())
                .maHoaDon(order.getMaHoaDon())
                .loaiDon(order.getLoaiDon())
                .hinhThucNhanHang(order.getHinhThucNhanHang())
                .trangThai(order.getTrangThai())
                .ngayTao(order.getNgayTao())
                .ngayThanhToan(order.getNgayThanhToan())
                .ngayCapNhat(order.getNgayCapNhat())
                .tenKhachHang(order.getTenKhachHang())
                .soDienThoai(order.getSoDienThoai())
                .diaChiKhachHang(order.getDiaChiKhachHang())
                .ghiChu(OrderStatusMailService.stripCustomerEmailMarker(order.getGhiChu()))
                .phuongThucThanhToan(readPaymentMethod(order.getGhiChu()))
                .tongTienGoc(MoneyRoundingUtil.roundNonNegative(order.getTongTienGoc()))
                .soTienGiam(MoneyRoundingUtil.roundNonNegative(order.getSoTienGiam()))
                .phiVanChuyen(MoneyRoundingUtil.roundNonNegative(order.getPhiVanChuyen()))
                .tongTienThanhToan(MoneyRoundingUtil.roundNonNegative(order.getTongTienThanhToan()))
                .maVoucher(voucher == null ? null : voucher.getMaPgg())
                .tenVoucher(voucher == null ? null : voucher.getTenPgg())
                .voucherDisplay(voucher == null ? null : voucherDisplay(voucher))
                .voucherDiscountText(voucher == null ? null : voucherDiscountText(voucher))
                .items(items)
                .build();
    }

    private ShopOrderHistoryItemDTO toOrderHistoryItemDTO(HoaDonChiTiet item) {
        ChiTietSanPham variant = item.getChiTietSanPham();
        SanPham product = variant == null ? null : variant.getSanPham();
        String image = variant == null ? null : firstImage(variant);
        BigDecimal originalPrice = MoneyRoundingUtil.roundNonNegative(variant == null ? item.getDonGia() : variant.getDonGia());
        BigDecimal paidPrice = MoneyRoundingUtil.roundNonNegative(item.getDonGia());
        BigDecimal unitDiscount = MoneyRoundingUtil.roundNonNegative(originalPrice.subtract(paidPrice));
        return ShopOrderHistoryItemDTO.builder()
                .id(item.getId())
                .idSpct(variant == null ? null : variant.getIdSpct())
                .maSanPham(product == null ? null : product.getMaSp())
                .tenSanPham(product == null ? "Sản phẩm" : product.getTenSp())
                .mauSac(variant == null ? null : tenMauSac(variant.getMauSac()))
                .kichCo(variant == null ? null : tenKichCo(variant.getKichCo()))
                .soLuong(item.getSoLuong())
                .giaGoc(originalPrice)
                .donGia(paidPrice)
                .thanhTien(MoneyRoundingUtil.roundNonNegative(item.getThanhTien()))
                .dangGiamGia(unitDiscount.compareTo(BigDecimal.ZERO) > 0)
                .soTienGiam(unitDiscount)
                .hinhAnh(image)
                .imageUrl(imageUrl(image))
                .build();
    }

    private ShopVoucherDTO toVoucherDTO(PhieuGiamGia voucher, BigDecimal subtotal, String selectedCode) {
        BigDecimal condition = MoneyRoundingUtil.roundNonNegative(voucher.getDieuKienDonHang());
        BigDecimal needMore = MoneyRoundingUtil.roundNonNegative(condition.subtract(subtotal));
        boolean quantityAvailable = voucher.getSoLuong() == null
                || (voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung()) < voucher.getSoLuong();
        boolean applicable = quantityAvailable && needMore.compareTo(BigDecimal.ZERO) <= 0;
        String reason = null;
        if (!quantityAvailable) {
            reason = "Phiếu đã hết lượt sử dụng";
        } else if (needMore.compareTo(BigDecimal.ZERO) > 0) {
            reason = "Cần mua thêm " + needMore.toPlainString() + " đ để áp dụng";
        }
        BigDecimal discountBase = applicable ? subtotal : condition.max(subtotal);
        int used = voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung();
        Integer remain = voucher.getSoLuong() == null ? null : Math.max(0, voucher.getSoLuong() - used);
        return ShopVoucherDTO.builder()
                .id(voucher.getId())
                .maPgg(voucher.getMaPgg())
                .tenPgg(voucher.getTenPgg())
                .displayText(voucherDisplay(voucher))
                .discountText(voucherDiscountText(voucher))
                .loaiGiam(voucher.getLoaiGiam())
                .giaTri(isPercentVoucher(voucher.getLoaiGiam())
                        ? money(voucher.getGiaTri())
                        : MoneyRoundingUtil.roundNonNegative(voucher.getGiaTri()))
                .giaTriToiDa(MoneyRoundingUtil.roundNonNegative(voucher.getGiaTriToiDa()))
                .dieuKienDonHang(condition)
                .soTienGiam(calculateDiscount(voucher, discountBase))
                .canMuaThem(needMore)
                .soLuong(voucher.getSoLuong())
                .soLuongDaDung(used)
                .soLuongConLai(remain)
                .ngayBatDau(voucher.getNgayBatDau())
                .ngayKetThuc(voucher.getNgayKetThuc())
                .applicable(applicable)
                .selected(selectedCode != null && selectedCode.equalsIgnoreCase(voucher.getMaPgg()))
                .reason(reason)
                .build();
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

    private boolean isVoucherInSaleWindow(PhieuGiamGia voucher) {
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() != null && now.isBefore(voucher.getNgayBatDau())) return false;
        if (voucher.getNgayKetThuc() != null && now.isAfter(voucher.getNgayKetThuc())) return false;
        return true;
    }

    private BigDecimal calculateDiscount(PhieuGiamGia voucher, BigDecimal subtotal) {
        BigDecimal value = isPercentVoucher(voucher.getLoaiGiam())
                ? money(voucher.getGiaTri())
                : MoneyRoundingUtil.roundNonNegative(voucher.getGiaTri());
        if (value.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal discount;
        if (isPercentVoucher(voucher.getLoaiGiam())) {
            discount = subtotal.multiply(value)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getGiaTriToiDa() != null && voucher.getGiaTriToiDa().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(MoneyRoundingUtil.roundNonNegative(voucher.getGiaTriToiDa()));
            }
        } else {
            discount = value;
        }
        return MoneyRoundingUtil.roundNonNegative(discount.min(subtotal));
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
        );
        text = normalize(text);
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

    private BigDecimal salePrice(ChiTietSanPham variant) {
        BigDecimal original = MoneyRoundingUtil.roundNonNegative(variant.getDonGia());
        return activeCampaign(variant)
                .map(campaign -> campaignPrice(original, campaign))
                .orElse(original);
    }

    private Optional<DotGiamGia> activeCampaign(ChiTietSanPham variant) {
        BigDecimal original = MoneyRoundingUtil.roundNonNegative(variant.getDonGia());
        return chiTietDotGiamGiaRepo.findActivePromotionsByVariantId(variant.getIdSpct(), LocalDateTime.now()).stream()
                .filter(campaign -> campaignPrice(original, campaign).compareTo(original) < 0)
                .min(Comparator.comparing(campaign -> campaignPrice(original, campaign)));
    }

    private BigDecimal campaignPrice(BigDecimal original, DotGiamGia campaign) {
        BigDecimal value = "PHAN_TRAM".equalsIgnoreCase(campaign.getLoaiGiamGia())
                ? money(campaign.getGiaTriGiamGia())
                : MoneyRoundingUtil.roundNonNegative(campaign.getGiaTriGiamGia());
        if (value.compareTo(BigDecimal.ZERO) <= 0) return original;
        boolean percentDiscount = "PHAN_TRAM".equalsIgnoreCase(campaign.getLoaiGiamGia());
        BigDecimal reduction = percentDiscount
                ? original.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : value;
        if (percentDiscount && campaign.getSoTienToiDa() != null && campaign.getSoTienToiDa().compareTo(BigDecimal.ZERO) > 0) {
            reduction = reduction.min(MoneyRoundingUtil.roundNonNegative(campaign.getSoTienToiDa()));
        }
        return MoneyRoundingUtil.roundNonNegative(original.subtract(reduction));
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

    private String firstImage(ChiTietSanPham variant) {
        String mainImage = trimToNull(variant.getHinhAnh());
        if (mainImage != null) return mainImage;

        String galleryImage = firstGalleryImage(variant.getDanhSachHinhAnh());
        if (galleryImage != null) return galleryImage;

        return firstImage(variant.getSanPham());
    }

    private String firstGalleryImage(String value) {
        String raw = trimToNull(value);
        if (raw == null) return null;

        String normalized = raw
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("\\/", "/");
        for (String item : normalized.split(",")) {
            String image = trimToNull(item);
            if (image != null) return image;
        }
        return null;
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

    private String tenXuatXu(XuatXu value) {
        return value == null ? null : value.getTenXuatXu();
    }

    private String tenPhongCach(PhongCachMac value) {
        return value == null ? null : value.getTenPhongCach();
    }

    private String tenKieuDang(KieuDang value) {
        return value == null ? null : value.getTenKieuDang();
    }

    private boolean isPercentVoucher(String type) {
        String normalized = normalize(type);
        return normalized != null
                && (normalized.contains("phan") || normalized.contains("percent") || normalized.contains("%"));
    }

    private String voucherDisplay(PhieuGiamGia voucher) {
        if (voucher == null) return null;
        String label = firstNonBlank(voucher.getTenPgg(), voucher.getMaPgg());
        String discount = voucherDiscountText(voucher);
        if (label == null) return discount;
        return discount == null ? label : label + " - " + discount;
    }

    private String voucherDiscountText(PhieuGiamGia voucher) {
        if (voucher == null) return null;
        BigDecimal value = money(voucher.getGiaTri());
        if (isPercentVoucher(voucher.getLoaiGiam())) {
            return stripMoney(value) + "%";
        }
        return formatMoneyText(MoneyRoundingUtil.roundNonNegative(value)) + " đ";
    }

    private String stripMoney(BigDecimal value) {
        return money(value).stripTrailingZeros().toPlainString();
    }

    private String formatMoneyText(BigDecimal value) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(money(value));
    }

    private String generateOrderCode(String customerName) {
        return GeneratedCodeUtil.fromNameAndDate(
                customerName,
                null,
                "HD",
                hoaDonRepo::existsByMaHoaDon
        );
    }

    private String normalizePhone(String value) {
        String phone = requireText(value, "Vui lòng nhập số điện thoại").replaceAll("\\D", "");
        if (!phone.matches("^(03|05|07|08|09)\\d{8}$")) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam");
        }
        return phone;
    }

    private String requireEmail(String value) {
        String email = requireText(value, "Vui long nhap email nhan thong tin don hang").toLowerCase(Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email khong hop le");
        }
        return email;
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
        if (trimmed == null) return null;

        return Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
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
            case "ONLINE", "VNPAY", "ZALOPAY", "BANKING" -> upper;
            default -> "COD";
        };
    }

    private boolean isGatewayPayment(String paymentMethod) {
        return "VNPAY".equals(paymentMethod)
                || "ZALOPAY".equals(paymentMethod)
                || "BANKING".equals(paymentMethod)
                || "ONLINE".equals(paymentMethod);
    }

    private String buildOrderNote(String note, String paymentMethod, String email) {
        String trimmedNote = trimToNull(note);
        String paymentLine = "Phương thức thanh toán: " + paymentMethod;
        String visibleNote = trimmedNote == null ? paymentLine : trimmedNote + "\n" + paymentLine;
        return OrderStatusMailService.appendCustomerEmailMarker(visibleNote, email);
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
