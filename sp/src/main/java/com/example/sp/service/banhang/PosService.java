package com.example.sp.service.banhang;

import com.example.sp.dto.banhang.PosCheckoutRequest;
import com.example.sp.dto.banhang.PosCustomerRequest;
import com.example.sp.dto.banhang.PosOrderDTO;
import com.example.sp.dto.banhang.PosOrderItemDTO;
import com.example.sp.dto.banhang.PosOrderItemRequest;
import com.example.sp.dto.banhang.PosShippingRequest;
import com.example.sp.dto.banhang.PosVoucherDTO;
import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.model.sanpham.HinhAnhSanPham;
import com.example.sp.model.sanpham.SanPham;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.HoaDonChiTiet;
import com.example.sp.model.hoadon.LichSuThanhToan;
import com.example.sp.model.hoadon.PhuongThucThanhToan;
import com.example.sp.model.hoadon.ThanhToan;
import com.example.sp.model.khuyenmai.DotGiamGia;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import com.example.sp.repository.khuyenmai.ChiTietDotGiamGiaRepository;
import com.example.sp.repository.sanpham.ChiTietSanPhamRepository;
import com.example.sp.repository.sanpham.HinhAnhSanPhamRepository;
import com.example.sp.repository.hoadon.HoaDonChiTietRepository;
import com.example.sp.repository.hoadon.HoaDonRepository;
import com.example.sp.repository.hoadon.LichSuThanhToanRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.repository.khuyenmai.PhieuGiamGiaRepository;
import com.example.sp.repository.hoadon.PhuongThucThanhToanRepository;
import com.example.sp.repository.hoadon.ThanhToanRepository;
import com.example.sp.service.tienich.GeneratedCodeUtil;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.service.tonkho.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PosService {

    private static final String POS_ORDER_TYPE = "Tại quầy";
    private static final String DRAFT_STATUS = "Chờ thanh toán";
    private static final int MAX_PENDING_ORDERS = 5;

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hoaDonChiTietRepo;
    private final ChiTietSanPhamRepository chiTietSanPhamRepo;
    private final KhachHangRepository khachHangRepo;
    private final PhieuGiamGiaRepository voucherRepo;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepo;
    private final HinhAnhSanPhamRepository hinhAnhRepo;
    private final ThanhToanRepository thanhToanRepo;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepo;
    private final LichSuThanhToanRepository lichSuThanhToanRepo;
    private final NhanVienRepository nhanVienRepo;
    private final InventoryService inventoryService;

    @Transactional
    public PosOrderDTO createOrder(Integer employeeId) {
        if (hoaDonRepo.countByLoaiDonAndTrangThai(POS_ORDER_TYPE, DRAFT_STATUS) >= MAX_PENDING_ORDERS) {
            throw new IllegalArgumentException("Chỉ được tạo tối đa 5 hóa đơn chờ");
        }
        NhanVien employee = employeeId == null ? null : nhanVienRepo.findById(employeeId).orElse(null);
        HoaDon order = HoaDon.builder()
                .maHoaDon(generateOrderCode("Khach le"))
                .nhanVien(employee)
                .loaiDon(POS_ORDER_TYPE)
                .hinhThucNhanHang(POS_ORDER_TYPE)
                .phiVanChuyen(BigDecimal.ZERO)
                .tongTienGoc(BigDecimal.ZERO)
                .soTienGiam(BigDecimal.ZERO)
                .tongTienThanhToan(BigDecimal.ZERO)
                .tenKhachHang("Khách lẻ")
                .trangThai(DRAFT_STATUS)
                .ngayTao(LocalDateTime.now())
                .ngayCapNhat(LocalDateTime.now())
                .build();
        return toDTO(hoaDonRepo.save(order), BigDecimal.ZERO);
    }

    @Transactional
    public List<PosOrderDTO> pendingOrders() {
        return hoaDonRepo.findByLoaiDonAndTrangThaiOrderByNgayTaoAsc(POS_ORDER_TYPE, DRAFT_STATUS).stream()
                .map(order -> {
                    refreshDraftPrices(order);
                    return toDTO(order, BigDecimal.ZERO);
                })
                .toList();
    }

    @Transactional
    public PosOrderDTO getOrder(Integer id) {
        HoaDon order = findOrder(id);
        if (DRAFT_STATUS.equals(order.getTrangThai())) {
            refreshDraftPrices(order);
        }
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO addItem(Integer idHoaDon, PosOrderItemRequest request) {
        HoaDon order = draftOrder(idHoaDon);
        ChiTietSanPham variant = findSellableVariant(request.getIdSpct());
        int requestedQty = request.getSoLuong();
        reserveStock(variant, requestedQty);

        HoaDonChiTiet detail = hoaDonChiTietRepo.findByHoaDon_Id(order.getId()).stream()
                .filter(item -> Objects.equals(item.getChiTietSanPham().getIdSpct(), variant.getIdSpct()))
                .findFirst()
                .orElse(null);
        if (detail == null) {
            detail = HoaDonChiTiet.builder()
                    .hoaDon(order)
                    .chiTietSanPham(variant)
                    .donGia(salePrice(variant))
                    .soLuong(requestedQty)
                    .build();
        } else {
            detail.setSoLuong((detail.getSoLuong() == null ? 0 : detail.getSoLuong()) + requestedQty);
            detail.setDonGia(salePrice(variant));
        }
        detail.setThanhTien(MoneyRoundingUtil.roundNonNegative(
                detail.getDonGia().multiply(BigDecimal.valueOf(detail.getSoLuong()))));
        hoaDonChiTietRepo.save(detail);
        autoApplyBestVoucher(order);
        recalculate(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO updateItem(Integer idHoaDon, Integer idHdct, Integer soLuong) {
        HoaDon order = draftOrder(idHoaDon);
        if (soLuong == null || soLuong < 1) throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        HoaDonChiTiet detail = findOrderItem(order, idHdct);
        ChiTietSanPham variant = detail.getChiTietSanPham();
        int currentQty = detail.getSoLuong() == null ? 0 : detail.getSoLuong();
        int delta = soLuong - currentQty;
        if (delta > 0) reserveStock(variant, delta);
        if (delta < 0) releaseStock(variant, -delta);
        detail.setSoLuong(soLuong);
        detail.setDonGia(salePrice(variant));
        detail.setThanhTien(MoneyRoundingUtil.roundNonNegative(
                detail.getDonGia().multiply(BigDecimal.valueOf(soLuong))));
        hoaDonChiTietRepo.save(detail);
        autoApplyBestVoucher(order);
        recalculate(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO removeItem(Integer idHoaDon, Integer idHdct) {
        HoaDon order = draftOrder(idHoaDon);
        HoaDonChiTiet detail = findOrderItem(order, idHdct);
        releaseStock(detail.getChiTietSanPham(), detail.getSoLuong() == null ? 0 : detail.getSoLuong());
        hoaDonChiTietRepo.delete(detail);
        autoApplyBestVoucher(order);
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
        syncOrderCodeWithCustomer(order);
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepo.save(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO setShipping(Integer idHoaDon, PosShippingRequest request) {
        HoaDon order = draftOrder(idHoaDon);
        if (!Boolean.TRUE.equals(request.getGiaoHang())) {
            order.setLoaiDon(POS_ORDER_TYPE);
            order.setHinhThucNhanHang(POS_ORDER_TYPE);
            order.setPhiVanChuyen(BigDecimal.ZERO);
            recalculate(order);
            return toDTO(order, BigDecimal.ZERO);
        }

        String phone = requireText(request.getSoDienThoai(), "Vui lòng nhập số điện thoại giao hàng").replaceAll("\\D", "");
        if (!phone.matches("^(03|05|07|08|09)\\d{8}$")) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam");
        }
        String detail = requireText(request.getDiaChiCuThe(), "Vui lòng nhập địa chỉ cụ thể");
        String ward = requireText(request.getWardName(), "Vui lòng chọn xã/phường");
        String province = requireText(request.getProvinceName(), "Vui lòng chọn tỉnh/thành phố");
        BigDecimal fee = MoneyRoundingUtil.roundNonNegative(request.getPhiVanChuyen());
        if (fee.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Phí vận chuyển không hợp lệ");

        order.setLoaiDon(POS_ORDER_TYPE);
        order.setHinhThucNhanHang("Giao hàng");
        String name = firstNonBlank(request.getTenKhachHang(), order.getTenKhachHang());
        order.setTenKhachHang(name == null ? "Khách lẻ" : name);
        order.setSoDienThoai(phone);
        order.setDiaChiKhachHang(String.join(", ", detail, ward, province));
        order.setGhiChu(firstNonBlank(request.getGhiChu(), order.getGhiChu()));
        order.setPhiVanChuyen(fee);
        recalculate(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional
    public PosOrderDTO applyVoucher(Integer idHoaDon, String maVoucher) {
        HoaDon order = draftOrder(idHoaDon);
        BigDecimal subtotal = subtotal(order);
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

    @Transactional(readOnly = true)
    public List<PosVoucherDTO> availableVouchers(Integer idHoaDon) {
        HoaDon order = draftOrder(idHoaDon);
        BigDecimal subtotal = subtotal(order);
        String selectedCode = order.getPhieuGiamGia() == null ? null : order.getPhieuGiamGia().getMaPgg();
        return voucherRepo.findByTrangThaiTrue().stream()
                .filter(this::isVoucherAlive)
                .map(voucher -> toVoucherDTO(voucher, subtotal, selectedCode))
                .sorted((left, right) -> {
                    int usable = Boolean.compare(right.isApplicable(), left.isApplicable());
                    if (usable != 0) return usable;
                    int discount = money(right.getSoTienGiam()).compareTo(money(left.getSoTienGiam()));
                    if (discount != 0) return discount;
                    return money(left.getCanMuaThem()).compareTo(money(right.getCanMuaThem()));
                })
                .toList();
    }

    @Transactional
    public PosOrderDTO checkout(Integer idHoaDon, PosCheckoutRequest request, Integer employeeId) {
        HoaDon order = draftOrder(idHoaDon);
        List<HoaDonChiTiet> items = hoaDonChiTietRepo.findByHoaDon_Id(order.getId());
        if (items.isEmpty()) throw new IllegalArgumentException("Đơn hàng chưa có sản phẩm");
        recalculate(order);

        PaymentMethodInfo requestedMethod = paymentMethodInfo(request.getPhuongThucThanhToan(), null);
        BigDecimal cash = MoneyRoundingUtil.roundNonNegative(request.getTienMat());
        BigDecimal transfer = MoneyRoundingUtil.roundNonNegative(request.getChuyenKhoan());
        if ("CHUYEN_KHOAN".equals(requestedMethod.code())) {
            transfer = transfer.compareTo(BigDecimal.ZERO) > 0 ? transfer : cash;
            cash = BigDecimal.ZERO;
        } else if ("TIEN_MAT".equals(requestedMethod.code())) {
            cash = cash.compareTo(BigDecimal.ZERO) > 0 ? cash : transfer;
            transfer = BigDecimal.ZERO;
        }
        BigDecimal paid = cash.add(transfer);
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            paid = MoneyRoundingUtil.roundNonNegative(request.getKhachThanhToan());
        }
        if (paid.compareTo(order.getTongTienThanhToan()) < 0) {
            throw new IllegalArgumentException("Khách thanh toán chưa đủ tiền");
        }

        PhieuGiamGia voucher = order.getPhieuGiamGia();
        if (voucher != null) {
            validateVoucher(voucher, subtotal(order));
            voucher.setSoLuongDaDung((voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung()) + 1);
            voucherRepo.save(voucher);
        }

        if (order.getNhanVien() == null && employeeId != null) {
            nhanVienRepo.findById(employeeId).ifPresent(order::setNhanVien);
        }
        syncOrderCodeWithCustomer(order);
        boolean shippingOrder = isShippingOrder(order);
        order.setTrangThai(shippingOrder ? "Đang chuẩn bị hàng" : "Hoàn thành");
        order.setNgayThanhToan(LocalDateTime.now());
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepo.save(order);

        savePayment(order, "TIEN_MAT", "Tiền mặt", cash);
        savePayment(order, "CHUYEN_KHOAN", "Chuyển khoản", transfer);
        if (cash.add(transfer).compareTo(BigDecimal.ZERO) <= 0) {
            savePayment(order, requestedMethod.code(), requestedMethod.name(), paid);
        }
        return toDTO(order, paid);
    }

    @Transactional
    public PosOrderDTO cancel(Integer idHoaDon) {
        HoaDon order = draftOrder(idHoaDon);
        hoaDonChiTietRepo.findByHoaDon_Id(order.getId())
                .forEach(item -> releaseStock(item.getChiTietSanPham(), item.getSoLuong() == null ? 0 : item.getSoLuong()));
        order.setTrangThai("Đã hủy");
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepo.save(order);
        return toDTO(order, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public PosOrderItemDTO findVariantByCode(String code) {
        String normalizedCode = requireText(code, "Vui lòng nhập mã sản phẩm");
        ChiTietSanPham variant = findVariantByQrText(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        if (!Boolean.TRUE.equals(variant.getTrangThai()) || !Boolean.TRUE.equals(variant.getSanPham().getTrangThai())) {
            throw new IllegalArgumentException("Sản phẩm đã ngừng bán");
        }
        return toVariantDTO(variant);
    }

    private Optional<ChiTietSanPham> findVariantByQrText(String code) {
        Optional<ChiTietSanPham> bySku = chiTietSanPhamRepo.findByMaChiTietSanPham(code);
        if (bySku.isPresent()) return bySku;
        String upper = code.toUpperCase(Locale.ROOT);
        if (upper.startsWith("SP-")) {
            try {
                return chiTietSanPhamRepo.findById(Integer.parseInt(code.substring(3).trim()));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private HoaDon findOrder(Integer id) {
        return hoaDonRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
    }

    private HoaDon draftOrder(Integer id) {
        HoaDon order = findOrder(id);
        if (!DRAFT_STATUS.equals(order.getTrangThai())) {
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

    private void reserveStock(ChiTietSanPham variant, int quantity) {
        inventoryService.reserveAtCounter(variant.getIdSpct(), quantity);
    }

    private void releaseStock(ChiTietSanPham variant, int quantity) {
        if (quantity <= 0) return;
        inventoryService.restoreStock(variant.getIdSpct(), quantity);
    }

    private void recalculate(HoaDon order) {
        BigDecimal subtotal = subtotal(order);
        BigDecimal discount = BigDecimal.ZERO;
        if (order.getPhieuGiamGia() != null && isVoucherUsable(order.getPhieuGiamGia(), subtotal)) {
            discount = calculateDiscount(order.getPhieuGiamGia(), subtotal);
        } else if (order.getPhieuGiamGia() != null) {
            order.setPhieuGiamGia(null);
        }
        order.setTongTienGoc(subtotal);
        order.setSoTienGiam(discount);
        BigDecimal shippingFee = MoneyRoundingUtil.roundNonNegative(order.getPhiVanChuyen());
        order.setPhiVanChuyen(shippingFee);
        order.setTongTienThanhToan(MoneyRoundingUtil.roundNonNegative(subtotal.subtract(discount).add(shippingFee)));
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepo.save(order);
    }

    private void refreshDraftPrices(HoaDon order) {
        boolean changed = false;
        for (HoaDonChiTiet item : hoaDonChiTietRepo.findByHoaDon_Id(order.getId())) {
            BigDecimal currentPrice = MoneyRoundingUtil.roundNonNegative(item.getDonGia());
            BigDecimal activePrice = salePrice(item.getChiTietSanPham());
            int quantity = item.getSoLuong() == null ? 0 : item.getSoLuong();
            BigDecimal activeLineTotal = MoneyRoundingUtil.roundNonNegative(
                    activePrice.multiply(BigDecimal.valueOf(quantity)));
            if (currentPrice.compareTo(activePrice) != 0 || money(item.getThanhTien()).compareTo(activeLineTotal) != 0) {
                item.setDonGia(activePrice);
                item.setThanhTien(activeLineTotal);
                hoaDonChiTietRepo.save(item);
                changed = true;
            }
        }
        if (changed) {
            recalculate(order);
        }
    }

    private BigDecimal subtotal(HoaDon order) {
        BigDecimal total = hoaDonChiTietRepo.findByHoaDon_Id(order.getId()).stream()
                .map(item -> MoneyRoundingUtil.roundNonNegative(item.getDonGia())
                        .multiply(BigDecimal.valueOf(item.getSoLuong() == null ? 0 : item.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return MoneyRoundingUtil.roundNonNegative(total);
    }

    /**
     * Campaigns are evaluated at the POS, while the variant's stored unit price
     * remains the original price. If campaigns overlap, the best final price wins.
     */
    private BigDecimal salePrice(ChiTietSanPham variant) {
        BigDecimal original = MoneyRoundingUtil.roundNonNegative(variant.getDonGia());
        return chiTietDotGiamGiaRepo.findActivePromotionsByVariantId(variant.getIdSpct(), LocalDateTime.now()).stream()
                .map(campaign -> campaignPrice(original, campaign))
                .min(BigDecimal::compareTo)
                .orElse(original);
    }

    private BigDecimal campaignPrice(BigDecimal original, DotGiamGia campaign) {
        BigDecimal value = "PHAN_TRAM".equalsIgnoreCase(campaign.getLoaiGiamGia())
                ? money(campaign.getGiaTriGiamGia())
                : MoneyRoundingUtil.roundNonNegative(campaign.getGiaTriGiamGia());
        if (value.compareTo(BigDecimal.ZERO) <= 0) return original;

        BigDecimal reduction = "PHAN_TRAM".equalsIgnoreCase(campaign.getLoaiGiamGia())
                ? original.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : value;
        if (campaign.getSoTienToiDa() != null && campaign.getSoTienToiDa().compareTo(BigDecimal.ZERO) > 0) {
            reduction = reduction.min(MoneyRoundingUtil.roundNonNegative(campaign.getSoTienToiDa()));
        }
        return MoneyRoundingUtil.roundNonNegative(original.subtract(reduction));
    }

    private void autoApplyBestVoucher(HoaDon order) {
        order.setPhieuGiamGia(bestVoucher(subtotal(order)).orElse(null));
    }

    private Optional<PhieuGiamGia> bestVoucher(BigDecimal subtotal) {
        return voucherRepo.findByTrangThaiTrue().stream()
                .filter(voucher -> isVoucherUsable(voucher, subtotal))
                .max((left, right) -> calculateDiscount(left, subtotal).compareTo(calculateDiscount(right, subtotal)));
    }

    private Optional<PhieuGiamGia> nextBetterVoucher(BigDecimal subtotal, BigDecimal currentDiscount) {
        return voucherRepo.findByTrangThaiTrue().stream()
                .filter(this::isVoucherAlive)
                .filter(voucher -> voucher.getDieuKienDonHang() != null
                        && MoneyRoundingUtil.roundNonNegative(voucher.getDieuKienDonHang()).compareTo(subtotal) > 0)
                .filter(voucher -> calculateDiscount(voucher,
                        MoneyRoundingUtil.roundNonNegative(voucher.getDieuKienDonHang())).compareTo(currentDiscount) > 0)
                .min((left, right) -> MoneyRoundingUtil.roundNonNegative(left.getDieuKienDonHang()).subtract(subtotal)
                        .compareTo(MoneyRoundingUtil.roundNonNegative(right.getDieuKienDonHang()).subtract(subtotal)));
    }

    private void validateVoucher(PhieuGiamGia voucher, BigDecimal subtotal) {
        if (!isVoucherAlive(voucher)) throw new IllegalArgumentException("Mã giảm giá không khả dụng");
        if (voucher.getDieuKienDonHang() != null
                && subtotal.compareTo(MoneyRoundingUtil.roundNonNegative(voucher.getDieuKienDonHang())) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt điều kiện của mã giảm giá");
        }
    }

    private boolean isVoucherUsable(PhieuGiamGia voucher, BigDecimal subtotal) {
        return isVoucherAlive(voucher)
                && (voucher.getDieuKienDonHang() == null
                || subtotal.compareTo(MoneyRoundingUtil.roundNonNegative(voucher.getDieuKienDonHang())) >= 0);
    }

    private boolean isVoucherAlive(PhieuGiamGia voucher) {
        if (!Boolean.TRUE.equals(voucher.getTrangThai())) return false;
        if (!isVoucherInSaleWindow(voucher)) return false;
        if (voucher.getSoLuong() != null) {
            int used = voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung();
            return used < voucher.getSoLuong();
        }
        return true;
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
            discount = subtotal.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getGiaTriToiDa() != null && voucher.getGiaTriToiDa().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(MoneyRoundingUtil.roundNonNegative(voucher.getGiaTriToiDa()));
            }
        } else {
            discount = value;
        }
        return MoneyRoundingUtil.roundNonNegative(discount.min(subtotal));
    }

    private PosVoucherDTO toVoucherDTO(PhieuGiamGia voucher, BigDecimal subtotal, String selectedCode) {
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
        return PosVoucherDTO.builder()
                .id(voucher.getId())
                .maVoucher(voucher.getMaPgg())
                .tenVoucher(voucher.getTenPgg())
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

    private PosOrderDTO toDTO(HoaDon order, BigDecimal paid) {
        List<PosOrderItemDTO> items = hoaDonChiTietRepo.findByHoaDon_Id(order.getId()).stream()
                .map(this::toItemDTO)
                .toList();
        BigDecimal total = MoneyRoundingUtil.roundNonNegative(order.getTongTienThanhToan());
        BigDecimal safePaid = MoneyRoundingUtil.roundNonNegative(paid);
        BigDecimal subtotal = MoneyRoundingUtil.roundNonNegative(order.getTongTienGoc());
        PhieuGiamGia hint = nextBetterVoucher(subtotal, MoneyRoundingUtil.roundNonNegative(order.getSoTienGiam())).orElse(null);
        BigDecimal hintOrderValue = hint == null ? null : MoneyRoundingUtil.roundNonNegative(hint.getDieuKienDonHang());
        BigDecimal hintNeedMore = hintOrderValue == null ? null : MoneyRoundingUtil.roundNonNegative(hintOrderValue.subtract(subtotal));
        BigDecimal hintDiscount = hint == null ? null : calculateDiscount(hint, hintOrderValue);
        return PosOrderDTO.builder()
                .id(order.getId())
                .maHoaDon(order.getMaHoaDon())
                // Giữ giá trị hiển thị cũ để màn POS không bị ảnh hưởng.
                .loaiDon(isShippingOrder(order) ? "Giao hàng" : POS_ORDER_TYPE)
                .hinhThucNhanHang(fulfillmentMethod(order))
                .trangThai(order.getTrangThai())
                .ngayTao(order.getNgayTao())
                .tenKhachHang(order.getTenKhachHang())
                .soDienThoai(order.getSoDienThoai())
                .diaChiKhachHang(order.getDiaChiKhachHang())
                .phiVanChuyen(MoneyRoundingUtil.roundNonNegative(order.getPhiVanChuyen()))
                .idKhachHang(order.getKhachHang() == null ? null : order.getKhachHang().getId())
                .maVoucher(order.getPhieuGiamGia() == null ? null : order.getPhieuGiamGia().getMaPgg())
                .tenVoucher(order.getPhieuGiamGia() == null ? null : order.getPhieuGiamGia().getTenPgg())
                .voucherDisplay(order.getPhieuGiamGia() == null ? null : voucherDisplay(order.getPhieuGiamGia()))
                .voucherDiscountText(order.getPhieuGiamGia() == null ? null : voucherDiscountText(order.getPhieuGiamGia()))
                .tongTienGoc(subtotal)
                .soTienGiam(MoneyRoundingUtil.roundNonNegative(order.getSoTienGiam()))
                .tongTienThanhToan(total)
                .khachThanhToan(safePaid)
                .tienThua(MoneyRoundingUtil.roundNonNegative(safePaid.subtract(total)))
                .voucherHintCode(hint == null ? null : hint.getMaPgg())
                .voucherHintNeedMore(hintNeedMore)
                .voucherHintDiscount(hintDiscount)
                .voucherHintOrderValue(hintOrderValue)
                .items(items)
                .build();
    }

    private boolean isShippingOrder(HoaDon order) {
        return "Giao hàng".equalsIgnoreCase(fulfillmentMethod(order));
    }

    private String fulfillmentMethod(HoaDon order) {
        if (order != null && order.getHinhThucNhanHang() != null
                && !order.getHinhThucNhanHang().isBlank()) {
            return order.getHinhThucNhanHang();
        }
        return order != null && "Giao hàng".equalsIgnoreCase(order.getLoaiDon())
                ? "Giao hàng"
                : POS_ORDER_TYPE;
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
                .giaGoc(MoneyRoundingUtil.roundNonNegative(variant.getDonGia()))
                .donGia(MoneyRoundingUtil.roundNonNegative(item.getDonGia()))
                .soLuong(item.getSoLuong())
                .soLuongTon(variant.getSoLuongKhaDung())
                .thanhTien(MoneyRoundingUtil.roundNonNegative(item.getDonGia())
                        .multiply(BigDecimal.valueOf(item.getSoLuong() == null ? 0 : item.getSoLuong())))
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
                .donGia(MoneyRoundingUtil.roundNonNegative(variant.getDonGia()))
                .soLuongTon(variant.getSoLuongKhaDung())
                .build();
    }

    private void savePayment(HoaDon order, String code, String name, BigDecimal amount) {
        BigDecimal value = MoneyRoundingUtil.roundNonNegative(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0) return;
        PhuongThucThanhToan method = findPaymentMethod(code, name);
        LocalDateTime paidAt = LocalDateTime.now();
        String transactionCode = "POS" + System.currentTimeMillis();
        thanhToanRepo.save(ThanhToan.builder()
                .hoaDon(order)
                .phuongThucThanhToan(method)
                .maGiaoDich(transactionCode)
                .soTien(value)
                .trangThai("Thành công")
                .thoiGianThanhToan(paidAt)
                .build());
        lichSuThanhToanRepo.save(LichSuThanhToan.builder()
                .hoaDon(order)
                .maGiaoDich(transactionCode)
                .soTien(value)
                .ngayThanhToan(paidAt)
                .hinhThucThanhToan(method.getTenPttt())
                .loaiThanhToan("Thanh toán tại quầy")
                .trangThai("Thành công")
                .build());
    }

    private PhuongThucThanhToan findPaymentMethod(String code, String name) {
        PaymentMethodInfo info = paymentMethodInfo(code, name);
        String safeCode = info.code();
        String safeName = info.name();
        return phuongThucThanhToanRepo.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase(safeCode, safeName)
                .map(method -> {
                    if (!safeName.equals(method.getTenPttt())) {
                        method.setTenPttt(safeName);
                        return phuongThucThanhToanRepo.save(method);
                    }
                    return method;
                })
                .orElseGet(() -> phuongThucThanhToanRepo.save(PhuongThucThanhToan.builder()
                        .maPttt(safeCode)
                        .tenPttt(safeName)
                        .trangThai(true)
                        .build()));
    }

    private PaymentMethodInfo paymentMethodInfo(String code, String name) {
        String raw = firstNonBlank(code, name, "TIEN_MAT");
        String normalized = normalizePaymentText(raw);
        if (normalized.contains("transfer") || normalized.contains("chuyen khoan") || normalized.contains("bank")
                || normalized.equals("ck")) {
            return new PaymentMethodInfo("CHUYEN_KHOAN", "Chuyển khoản");
        }
        if (normalized.contains("mixed") || normalized.contains("ket hop")) {
            return new PaymentMethodInfo("KET_HOP", "Tiền mặt + Chuyển khoản");
        }
        if (normalized.contains("cash") || normalized.contains("tien mat") || normalized.equals("tm")) {
            return new PaymentMethodInfo("TIEN_MAT", "Tiền mặt");
        }
        return new PaymentMethodInfo(raw.trim().toUpperCase(Locale.ROOT), trimToNull(name) == null ? raw.trim() : name.trim());
    }

    private String firstNonBlank(String first, String second, String fallback) {
        String value = trimToNull(first);
        if (value != null) return value;
        value = trimToNull(second);
        return value == null ? fallback : value;
    }

    private String normalizePaymentText(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return "";
        return Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replace('_', ' ')
                .replace('-', ' ')
                .toLowerCase(Locale.ROOT);
    }

    private record PaymentMethodInfo(String code, String name) {
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
        if (lower.startsWith("http://") || lower.startsWith("https://") || image.startsWith("/")) return image;
        return "/uploads/" + image;
    }

    private boolean isPercentVoucher(String type) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        return normalized.contains("phan") || normalized.contains("percent") || normalized.contains("%");
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

    private void syncOrderCodeWithCustomer(HoaDon order) {
        if (order == null) {
            return;
        }
        KhachHang customer = order.getKhachHang();
        String codeName = customer == null
                ? "Khach le"
                : firstNonBlank(customer.getTenKhachHang(), order.getTenKhachHang());
        order.setMaHoaDon(generateOrderCodeForOrder(codeName, order));
    }

    private String generateOrderCodeForOrder(String customerName, HoaDon order) {
        Integer currentOrderId = order == null ? null : order.getId();
        LocalDateTime createdAt = order == null ? null : order.getNgayTao();
        return GeneratedCodeUtil.fromNameAndDate(
                customerName,
                createdAt,
                "HD",
                code -> hoaDonRepo.findFirstByMaHoaDonIgnoreCase(code)
                        .filter(existing -> !Objects.equals(existing.getId(), currentOrderId))
                        .isPresent()
        );
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
