package com.example.sp.service.trienkhai;

import com.example.sp.dto.hoadon.HoaDonChiTietDTO;
import com.example.sp.dto.hoadon.ChiTietHoanKhoRequest;
import com.example.sp.dto.hoadon.XacNhanHoanHangRequest;
import com.example.sp.dto.hoadon.XuLyGiaoHangThatBaiRequest;
import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.HoaDonChiTiet;
import com.example.sp.model.hoadon.LichSuThanhToan;
import com.example.sp.model.hoadon.PhuongThucThanhToan;
import com.example.sp.model.hoadon.ThanhToan;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import com.example.sp.repository.hoadon.HoaDonChiTietRepository;
import com.example.sp.repository.hoadon.HoaDonRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.hoadon.LichSuThanhToanRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.repository.khuyenmai.PhieuGiamGiaRepository;
import com.example.sp.repository.hoadon.PhuongThucThanhToanRepository;
import com.example.sp.repository.hoadon.ThanhToanRepository;
import com.example.sp.service.hoadon.HoaDonService;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.service.hoadon.OrderStatusMailService;
import com.example.sp.service.tonkho.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HoaDonServiceImpl implements HoaDonService {

    private static final String STATUS_CANCELLED = "Đã hủy";
    private static final String STATUS_WAITING_RETURN = "Chờ hàng hoàn";
    private static final String DEFAULT_RETURN_REASON = "Khách không nhận hàng";
    private static final String FAILURE_CARRIER_LOST = "MAT_HANG_VAN_CHUYEN";
    private static final String FAILURE_CUSTOMER_REFUSED = "KHACH_KHONG_NHAN";
    private static final String FAILURE_WRONG_PRODUCT = "SHOP_GUI_SAI";
    private static final String REASON_CARRIER_LOST = "Đơn vị vận chuyển làm mất hàng";
    private static final String REASON_CUSTOMER_REFUSED = "Khách từ chối nhận hàng / bom hàng";
    private static final String REASON_WRONG_PRODUCT = "Shop gửi sai sản phẩm";

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository chiTietRepo;
    private final PhieuGiamGiaRepository voucherRepo;
    private final LichSuThanhToanRepository lichSuRepo;
    private final KhachHangRepository khachHangRepo;
    private final ThanhToanRepository thanhToanRepo;
    private final PhuongThucThanhToanRepository ptttRepo;
    private final NhanVienRepository nhanVienRepo;
    private final OrderStatusMailService orderStatusMailService;
    private final InventoryService inventoryService;

    @Override
    public Page<HoaDon> timKiem(String maHD, String tuNgay, String denNgay,
                                String loaiDon, String trangThai,
                                BigDecimal maxGia, Pageable pageable) {

        String maHDFix = (maHD == null || maHD.isBlank()) ? null : maHD.trim();

        String trangThaiFix = null;
        List<String> dsTrangThai;
        if (trangThai != null && !trangThai.isBlank()) {
            if (trangThai.contains("|")) {
                trangThaiFix = "multi";
                dsTrangThai = java.util.Arrays.asList(trangThai.split("\\|"));
            } else {
                trangThaiFix = trangThai.trim();
                dsTrangThai = java.util.Collections.singletonList(trangThaiFix);
            }
        } else {
            dsTrangThai = java.util.Collections.emptyList();
        }

        String loaiDonFix = null;
        if (loaiDon != null && !loaiDon.isBlank()) {
            String value = normalizeStatus(loaiDon);
            if ("tai quay".equals(value)) loaiDonFix = "Tại quầy";
            else if ("online".equals(value) || "truc tuyen".equals(value)) loaiDonFix = "Trực tuyến";
            else loaiDonFix = loaiDon.trim();
        }

        LocalDateTime from = (tuNgay == null || tuNgay.isBlank()) ? null
                : LocalDateTime.parse(tuNgay + "T00:00:00");
        LocalDateTime to = (denNgay == null || denNgay.isBlank()) ? null
                : LocalDateTime.parse(denNgay + "T23:59:59");

        return hoaDonRepo.timKiem(maHDFix, from, to, loaiDonFix, trangThaiFix, dsTrangThai, maxGia, pageable);
    }

    @Override
    public BigDecimal getGiaMax() {
        BigDecimal max = hoaDonRepo.findMaxTongTienThanhToan();
        return max != null ? max : BigDecimal.valueOf(10000000);
    }

    @Override
    @Transactional
    public HoaDon huyHoaDon(Integer idHoaDon) {
        HoaDon hd = findByIdForUpdate(idHoaDon);
        String trangThaiCu = hd.getTrangThai();
        if (isWaitingReturnStatus(trangThaiCu)) {
            throw new IllegalStateException("Hàng vẫn đang trên đường hoàn về. Hãy xác nhận nhập kho trước khi hoàn tất hủy đơn");
        }
        if (isInTransitStatus(trangThaiCu) && isDeliveryOrder(hd)) {
            return moveToWaitingReturn(hd, DEFAULT_RETURN_REASON);
        }
        if (isCancelledStatus(trangThaiCu)) {
            return hd;
        }
        if (shouldReleaseStockOnCancel(hd, trangThaiCu, STATUS_CANCELLED)) {
            releaseStockForOrder(hd, trangThaiCu);
        }
        hd.setTrangThai(STATUS_CANCELLED);
        hd.setNgayCapNhat(LocalDateTime.now());
        HoaDon saved = hoaDonRepo.save(hd);
        notifyStatusChanged(saved, trangThaiCu, saved.getTrangThai());
        return saved;
    }

    @Override
    @Transactional
    public HoaDon yeuCauHoanHang(Integer idHoaDon, String lyDo) {
        HoaDon hd = findByIdForUpdate(idHoaDon);
        return moveToWaitingReturn(hd, lyDo);
    }

    @Override
    @Transactional
    public HoaDon xuLyGiaoHangThatBai(Integer idHoaDon, XuLyGiaoHangThatBaiRequest request) {
        if (request == null || request.getLoaiSuCo() == null || request.getLoaiSuCo().isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn nguyên nhân giao hàng không thành công");
        }

        HoaDon hd = findByIdForUpdate(idHoaDon);
        String incidentType = normalizeIncidentType(request.getLoaiSuCo());
        String note = trimToLength(request.getGhiChu(), 1000);

        return switch (incidentType) {
            case FAILURE_CARRIER_LOST -> cancelCarrierLostOrder(hd, note);
            case FAILURE_CUSTOMER_REFUSED -> moveToWaitingReturn(
                    hd,
                    REASON_CUSTOMER_REFUSED,
                    defaultIfBlank(note,
                            "Đã gọi xác nhận khách không nhận hàng. Báo quản lý và chờ đơn quay đầu về kho.")
            );
            case FAILURE_WRONG_PRODUCT -> moveToWaitingReturn(
                    hd,
                    REASON_WRONG_PRODUCT,
                    defaultIfBlank(note,
                            "Liên hệ xin lỗi khách và tạo đơn giao đúng riêng. Đơn cũ chờ quay đầu về kho.")
            );
            default -> throw new IllegalArgumentException("Nguyên nhân giao hàng không thành công không hợp lệ");
        };
    }

    @Override
    @Transactional
    public HoaDon xacNhanHangHoan(Integer idHoaDon, XacNhanHoanHangRequest request) {
        HoaDon hd = findByIdForUpdate(idHoaDon);
        String trangThaiCu = hd.getTrangThai();
        if (!isWaitingReturnStatus(trangThaiCu)) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chờ hàng hoàn");
        }
        if (Boolean.TRUE.equals(hd.getDaHoanTon())) {
            throw new IllegalStateException("Đơn hàng đã được hoàn tồn trước đó");
        }
        if (request == null || request.getChiTiet() == null) {
            throw new IllegalArgumentException("Vui lòng nhập số lượng hàng thực tế nhận lại");
        }

        List<HoaDonChiTiet> items = chiTietRepo.findByHoaDon_Id(hd.getId());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn chưa có sản phẩm");
        }

        Map<Integer, Integer> returnedQuantities = new HashMap<>();
        for (ChiTietHoanKhoRequest detail : request.getChiTiet()) {
            if (detail == null || detail.getIdHdct() == null || detail.getSoLuongNhapKho() == null) {
                throw new IllegalArgumentException("Dữ liệu hàng hoàn không đầy đủ");
            }
            if (detail.getSoLuongNhapKho() < 0) {
                throw new IllegalArgumentException("Số lượng nhập lại kho không được âm");
            }
            if (returnedQuantities.putIfAbsent(detail.getIdHdct(), detail.getSoLuongNhapKho()) != null) {
                throw new IllegalArgumentException("Dòng sản phẩm hoàn kho bị trùng");
            }
        }
        if (returnedQuantities.size() != items.size()) {
            throw new IllegalArgumentException("Cần xác nhận số lượng hoàn kho cho từng sản phẩm trong đơn");
        }

        for (HoaDonChiTiet item : items) {
            Integer returnedQuantity = returnedQuantities.remove(item.getId());
            if (returnedQuantity == null) {
                throw new IllegalArgumentException("Có sản phẩm hoàn kho không thuộc hóa đơn");
            }
            int orderedQuantity = item.getSoLuong() == null ? 0 : item.getSoLuong();
            if (returnedQuantity > orderedQuantity) {
                throw new IllegalArgumentException("Số lượng nhập lại kho không được vượt quá số lượng đã giao");
            }
            if (returnedQuantity > 0) {
                ChiTietSanPham variant = item.getChiTietSanPham();
                if (variant == null || variant.getIdSpct() == null) {
                    throw new IllegalStateException("Không tìm thấy biến thể sản phẩm để hoàn kho");
                }
                inventoryService.restoreStock(variant.getIdSpct(), returnedQuantity);
            }
            item.setSoLuongHoanKho(returnedQuantity);
        }
        if (!returnedQuantities.isEmpty()) {
            throw new IllegalArgumentException("Có sản phẩm hoàn kho không thuộc hóa đơn");
        }
        chiTietRepo.saveAll(items);

        hd.setDaHoanTon(true);
        hd.setGhiChuHoanHang(trimToLength(request.getGhiChu(), 1000));
        hd.setNgayNhanHangHoan(LocalDateTime.now());
        hd.setTrangThai(STATUS_CANCELLED);
        hd.setNgayCapNhat(LocalDateTime.now());
        HoaDon saved = hoaDonRepo.save(hd);
        notifyStatusChanged(saved, trangThaiCu, saved.getTrangThai());
        return saved;
    }

    @Override
    @Transactional
    public HoaDon capNhatTrangThai(Integer idHoaDon, String trangThai, Integer idNhanVienThucHien) {
        HoaDon hd = findByIdForUpdate(idHoaDon);
        String trangThaiCu = hd.getTrangThai();
        if (trangThai == null || trangThai.isBlank()) {
            throw new IllegalArgumentException("Thiếu trạng thái hóa đơn");
        }
        if (isWaitingReturnStatus(trangThaiCu) && !isWaitingReturnStatus(trangThai)) {
            throw new IllegalStateException("Phải xác nhận hàng đã về kho trước khi kết thúc quy trình hoàn hàng");
        }
        if (isWaitingReturnStatus(trangThai)
                || (isCancelledStatus(trangThai) && isInTransitStatus(trangThaiCu) && isDeliveryOrder(hd))) {
            return moveToWaitingReturn(hd, DEFAULT_RETURN_REASON);
        }
        if (shouldConfirmStock(hd, trangThaiCu, trangThai)) {
            confirmStockForOrder(hd);
        }
        if (shouldReleaseStockOnCancel(hd, trangThaiCu, trangThai)) {
            releaseStockForOrder(hd, trangThaiCu);
        }
        if (shouldAssignConfirmingEmployee(hd, trangThaiCu, trangThai, idNhanVienThucHien)) {
            nhanVienRepo.findById(idNhanVienThucHien).ifPresent(hd::setNhanVien);
        }
        hd.setTrangThai(trangThai);
        if (isPaymentCompletedStatus(trangThai)) {
            hd.setNgayThanhToan(LocalDateTime.now());
        }
        hd.setNgayCapNhat(LocalDateTime.now());
        HoaDon saved = hoaDonRepo.save(hd);
        if (isPaymentCompletedStatus(trangThai)) {
            ensurePaymentHistory(saved, inferPaymentMethod(saved));
        }
        notifyStatusChanged(saved, trangThaiCu, trangThai);
        return saved;
    }

    private void notifyStatusChanged(HoaDon hd, String oldStatus, String newStatus) {
        if (normalizeStatus(oldStatus).equals(normalizeStatus(newStatus))) {
            return;
        }
        // Resolve the lazy customer relation while the transaction is still open.
        // SMTP delivery runs asynchronously so status updates are not blocked by email I/O.
        String customerEmail = orderStatusMailService.resolveCustomerEmail(hd);
        orderStatusMailService.sendStatusChanged(hd, oldStatus, newStatus, customerEmail);
    }

    private boolean shouldAssignConfirmingEmployee(HoaDon hd, String oldStatus, String newStatus, Integer employeeId) {
        return employeeId != null
                && isOnlineOrder(hd)
                && !isStatus(oldStatus, "da xac nhan")
                && isStatus(newStatus, "da xac nhan");
    }

    private boolean shouldConfirmStock(HoaDon hd, String oldStatus, String newStatus) {
        return isOnlineOrder(hd)
                && !isCancelledStatus(oldStatus)
                && !hasStockBeenDeducted(hd, oldStatus)
                && isStockDeductedStatus(newStatus);
    }

    private boolean shouldReleaseStockOnCancel(HoaDon hd, String oldStatus, String newStatus) {
        return isOnlineOrder(hd)
                && !isCancelledStatus(oldStatus)
                && !isInTransitStatus(oldStatus)
                && !isWaitingReturnStatus(oldStatus)
                && isCancelledStatus(newStatus)
                && (Boolean.TRUE.equals(hd.getDaGiuTon()) || hasStockBeenDeducted(hd, oldStatus));
    }

    private boolean hasStockBeenDeducted(HoaDon hd, String currentStatus) {
        if (hd.getDaTruTon() != null) {
            return Boolean.TRUE.equals(hd.getDaTruTon());
        }
        // Legacy invoices predate da_tru_ton. Preserve their original data and
        // infer the old behavior only while the explicit flag is NULL.
        return isStockDeductedStatus(currentStatus);
    }

    private boolean isOnlineOrder(HoaDon hd) {
        String normalized = normalizeStatus(hd.getLoaiDon());
        return normalized.contains("truc tuyen") || normalized.contains("online");
    }

    private boolean isDeliveryOrder(HoaDon hd) {
        String fulfillment = normalizeStatus(hd.getHinhThucNhanHang());
        String orderType = normalizeStatus(hd.getLoaiDon());
        return fulfillment.equals("giao hang")
                || orderType.equals("giao hang")
                || orderType.contains("truc tuyen")
                || orderType.contains("online");
    }

    private boolean isInTransitStatus(String status) {
        return normalizeStatus(status).equals("dang giao hang");
    }

    private boolean isWaitingReturnStatus(String status) {
        String normalized = normalizeStatus(status);
        return normalized.equals("cho hang hoan") || normalized.equals("cho hoan hang");
    }

    private boolean isStockDeductedStatus(String status) {
        String normalized = normalizeStatus(status);
        return normalized.equals("da xac nhan")
                || normalized.equals("dang chuan bi hang")
                || normalized.equals("dang giao hang")
                || normalized.equals("cho giao hang");
    }

    private boolean isCancelledStatus(String status) {
        String normalized = normalizeStatus(status);
        return normalized.equals("da huy")
                || normalized.equals("huy")
                || normalized.equals("huy don");
    }

    private HoaDon moveToWaitingReturn(HoaDon hd, String reason) {
        return moveToWaitingReturn(hd, reason, null);
    }

    private HoaDon moveToWaitingReturn(HoaDon hd, String reason, String note) {
        String oldStatus = hd.getTrangThai();
        if (isWaitingReturnStatus(oldStatus)) {
            return hd;
        }
        if (!isDeliveryOrder(hd) || !isInTransitStatus(oldStatus)) {
            throw new IllegalStateException("Chỉ đơn đang giao hàng mới được chuyển sang chờ hàng hoàn");
        }
        hd.setTrangThai(STATUS_WAITING_RETURN);
        hd.setLyDoHoanHang(defaultIfBlank(trimToLength(reason, 500), DEFAULT_RETURN_REASON));
        hd.setGhiChuHoanHang(trimToLength(note, 1000));
        hd.setDaHoanTon(false);
        hd.setNgayYeuCauHoan(LocalDateTime.now());
        hd.setNgayNhanHangHoan(null);
        hd.setNgayCapNhat(LocalDateTime.now());
        HoaDon saved = hoaDonRepo.save(hd);
        notifyStatusChanged(saved, oldStatus, saved.getTrangThai());
        return saved;
    }

    private HoaDon cancelCarrierLostOrder(HoaDon hd, String note) {
        String oldStatus = hd.getTrangThai();
        if (isCancelledStatus(oldStatus) && REASON_CARRIER_LOST.equals(hd.getLyDoHoanHang())) {
            return hd;
        }
        if (!isDeliveryOrder(hd) || !isInTransitStatus(oldStatus)) {
            throw new IllegalStateException("Chỉ đơn đang giao hàng mới được ghi nhận mất hàng do vận chuyển");
        }

        hd.setTrangThai(STATUS_CANCELLED);
        hd.setLyDoHoanHang(REASON_CARRIER_LOST);
        hd.setGhiChuHoanHang(defaultIfBlank(
                note,
                "Không hoàn tồn kho. Báo quản lý, lập hồ sơ và làm việc bồi thường với đơn vị vận chuyển."
        ));
        hd.setDaHoanTon(false);
        hd.setNgayYeuCauHoan(null);
        hd.setNgayNhanHangHoan(null);
        hd.setNgayCapNhat(LocalDateTime.now());
        HoaDon saved = hoaDonRepo.save(hd);
        notifyStatusChanged(saved, oldStatus, saved.getTrangThai());
        return saved;
    }

    private String normalizeIncidentType(String value) {
        String normalized = normalizeStatus(value)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MAT_HANG_VAN_CHUYEN", "SHIP_LAM_MAT_HANG", "VAN_CHUYEN_LAM_MAT_HANG" ->
                    FAILURE_CARRIER_LOST;
            case "KHACH_KHONG_NHAN", "KHACH_BOM_HANG", "BOM_HANG" ->
                    FAILURE_CUSTOMER_REFUSED;
            case "SHOP_GUI_SAI", "GUI_SAI_SAN_PHAM", "SAI_SAN_PHAM" ->
                    FAILURE_WRONG_PRODUCT;
            default -> normalized;
        };
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isStatus(String value, String expected) {
        return normalizeStatus(value).equals(expected);
    }

    private boolean isPaymentCompletedStatus(String status) {
        String normalized = normalizeStatus(status);
        return normalized.equals("da thanh toan")
                || normalized.equals("hoan thanh")
                || normalized.equals("hoan tat");
    }

    private String inferPaymentMethod(HoaDon hd) {
        String note = normalizeStatus(hd == null ? null : hd.getGhiChu());
        if (note.contains("chuyen khoan") || note.contains("chuyen_khoan") || note.contains("banking")
                || note.contains("bank") || note.contains("transfer")) {
            return "CHUYEN_KHOAN";
        }
        if (note.contains("online") || note.contains("vnpay") || note.contains("momo")
                || note.contains("zalopay") || note.contains("zalo pay")) {
            return "ONLINE";
        }
        return "COD";
    }

    private void ensurePaymentHistory(HoaDon hd, String paymentMethod) {
        if (hd == null || hd.getId() == null) return;
        LocalDateTime paidAt = hd.getNgayThanhToan() == null ? LocalDateTime.now() : hd.getNgayThanhToan();
        String methodCode = normalizePaymentCode(paymentMethod);
        String methodName = paymentDisplayName(methodCode, paymentMethod);

        if (thanhToanRepo.findByHoaDon_Id(hd.getId()).isEmpty()) {
            PhuongThucThanhToan pttt = findOrCreatePaymentMethod(methodCode, methodName);
            thanhToanRepo.save(ThanhToan.builder()
                    .hoaDon(hd)
                    .phuongThucThanhToan(pttt)
                    .maGiaoDich(methodCode + hd.getId() + System.currentTimeMillis())
                    .soTien(MoneyRoundingUtil.roundNonNegative(hd.getTongTienThanhToan()))
                    .trangThai("Thành công")
                    .thoiGianThanhToan(paidAt)
                    .build());
        }

        if (lichSuRepo.findByHoaDon_IdOrderByNgayThanhToanDesc(hd.getId()).isEmpty()) {
            lichSuRepo.save(LichSuThanhToan.builder()
                    .hoaDon(hd)
                    .maGiaoDich(methodCode + hd.getId() + System.currentTimeMillis())
                    .soTien(MoneyRoundingUtil.roundNonNegative(hd.getTongTienThanhToan()))
                    .ngayThanhToan(paidAt)
                    .hinhThucThanhToan(methodName)
                    .loaiThanhToan("Thanh toán hóa đơn")
                    .trangThai("Thành công")
                    .build());
        }
    }

    private String normalizeStatus(String value) {
        if (value == null) return "";
        return Normalizer.normalize(fixMojibake(value).trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private String fixMojibake(String value) {
        boolean looksMojibake = value.indexOf(0x00C3) >= 0
                || value.indexOf(0x00C4) >= 0
                || value.indexOf(0x00C2) >= 0
                || value.chars().anyMatch(ch -> ch >= 0x80 && ch <= 0x9F);
        if (!looksMojibake) return value;
        try {
            return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return value;
        }
    }

    private void confirmStockForOrder(HoaDon hd) {
        List<HoaDonChiTiet> items = chiTietRepo.findByHoaDon_Id(hd.getId());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn chưa có sản phẩm");
        }

        boolean hasOnlineReservation = Boolean.TRUE.equals(hd.getDaGiuTon());
        for (HoaDonChiTiet item : items) {
            ChiTietSanPham variant = item.getChiTietSanPham();
            int quantity = item.getSoLuong() == null ? 0 : item.getSoLuong();
            if (quantity <= 0) continue;
            if (hasOnlineReservation) {
                inventoryService.confirmOnlineReservation(variant.getIdSpct(), quantity);
            } else {
                inventoryService.deductLegacyOnlineStock(variant.getIdSpct(), quantity);
            }
        }
        hd.setDaGiuTon(false);
        hd.setDaTruTon(true);
    }

    private void releaseStockForOrder(HoaDon hd, String oldStatus) {
        List<HoaDonChiTiet> items = chiTietRepo.findByHoaDon_Id(hd.getId());
        boolean hasOnlineReservation = Boolean.TRUE.equals(hd.getDaGiuTon());
        boolean stockWasDeducted = hasStockBeenDeducted(hd, oldStatus);
        for (HoaDonChiTiet item : items) {
            ChiTietSanPham variant = item.getChiTietSanPham();
            int quantity = item.getSoLuong() == null ? 0 : item.getSoLuong();
            if (quantity <= 0) continue;
            if (hasOnlineReservation) {
                inventoryService.releaseOnlineReservation(variant.getIdSpct(), quantity);
            } else if (stockWasDeducted) {
                inventoryService.restoreStock(variant.getIdSpct(), quantity);
            }
        }
        hd.setDaGiuTon(false);
        if (stockWasDeducted) {
            hd.setDaTruTon(false);
        }
    }

    @Override public List<HoaDon> findAll() { return hoaDonRepo.findAll(); }
    @Override
    public HoaDon taoHoaDon(Integer idKh, Integer idNv) {
        HoaDon hd = HoaDon.builder()
                .maHoaDon(generateInvoiceCode())
                .ngayTao(LocalDateTime.now())
                .ngayCapNhat(LocalDateTime.now())
                .trangThai("Chờ thanh toán")
                .loaiDon("Tại quầy")
                .hinhThucNhanHang("Tại quầy")
                .tongTienGoc(BigDecimal.ZERO)
                .soTienGiam(BigDecimal.ZERO)
                .phiVanChuyen(BigDecimal.ZERO)
                .tongTienThanhToan(BigDecimal.ZERO)
                .build();
        if (idKh != null) {
            khachHangRepo.findById(idKh).ifPresent(customer -> {
                hd.setKhachHang(customer);
                hd.setTenKhachHang(customer.getTenKhachHang());
                hd.setSoDienThoai(customer.getSoDienThoai());
                hd.setDiaChiKhachHang(customer.getDiaChiDisplay());
            });
        }
        if (idNv != null) {
            nhanVienRepo.findById(idNv).ifPresent(hd::setNhanVien);
        }
        return hoaDonRepo.save(hd);
    }
    @Override
    @Transactional
    public void themSanPham(Integer idHoaDon, Integer idSpct, Integer soLuong) {
        if (soLuong == null || soLuong <= 0) {
            throw new IllegalArgumentException("So luong phai lon hon 0");
        }
        HoaDon hd = findById(idHoaDon);
        ChiTietSanPham variant = inventoryService.reserveAtCounter(idSpct, soLuong);
        HoaDonChiTiet item = chiTietRepo.findByHoaDon_Id(hd.getId()).stream()
                .filter(existing -> existing.getChiTietSanPham() != null
                        && idSpct.equals(existing.getChiTietSanPham().getIdSpct()))
                .findFirst()
                .orElseGet(() -> HoaDonChiTiet.builder()
                        .hoaDon(hd)
                        .chiTietSanPham(variant)
                        .soLuong(0)
                        .donGia(MoneyRoundingUtil.roundNonNegative(variant.getDonGia()))
                        .thanhTien(BigDecimal.ZERO)
                        .build());
        item.setSoLuong((item.getSoLuong() == null ? 0 : item.getSoLuong()) + soLuong);
        item.setDonGia(MoneyRoundingUtil.roundNonNegative(item.getDonGia()));
        item.setThanhTien(MoneyRoundingUtil.roundNonNegative(
                item.getDonGia().multiply(BigDecimal.valueOf(item.getSoLuong()))));
        chiTietRepo.save(item);
        recalculateInvoice(hd);
    }
    @Override
    @Transactional
    public void xoaSanPham(Integer idHdct) {
        HoaDonChiTiet item = chiTietRepo.findById(idHdct)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay dong san pham"));
        HoaDon hd = item.getHoaDon();
        ChiTietSanPham variant = item.getChiTietSanPham();
        if (variant != null) {
            int quantity = item.getSoLuong() == null ? 0 : item.getSoLuong();
            if (quantity > 0) {
                inventoryService.restoreStock(variant.getIdSpct(), quantity);
            }
        }
        chiTietRepo.delete(item);
        recalculateInvoice(hd);
    }
    @Override
    @Transactional
    public void capNhatSoLuong(Integer idHdct, Integer soLuong) {
        if (soLuong == null || soLuong <= 0) {
            xoaSanPham(idHdct);
            return;
        }
        HoaDonChiTiet item = chiTietRepo.findById(idHdct)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay dong san pham"));
        ChiTietSanPham variant = item.getChiTietSanPham();
        int oldQuantity = item.getSoLuong() == null ? 0 : item.getSoLuong();
        int delta = soLuong - oldQuantity;
        if (delta > 0) {
            inventoryService.reserveAtCounter(variant.getIdSpct(), delta);
        } else if (delta < 0) {
            inventoryService.restoreStock(variant.getIdSpct(), -delta);
        }
        item.setSoLuong(soLuong);
        item.setDonGia(MoneyRoundingUtil.roundNonNegative(item.getDonGia()));
        item.setThanhTien(MoneyRoundingUtil.roundNonNegative(
                item.getDonGia().multiply(BigDecimal.valueOf(soLuong))));
        chiTietRepo.save(item);
        recalculateInvoice(item.getHoaDon());
    }
    @Override
    @Transactional
    public BigDecimal tinhTongTien(Integer idHoaDon) {
        HoaDon hd = findById(idHoaDon);
        return recalculateInvoice(hd).getTongTienThanhToan();
    }
    @Override
    @Transactional
    public HoaDon apVoucher(Integer idHoaDon, Integer idVoucher) {
        HoaDon hd = findById(idHoaDon);
        PhieuGiamGia voucher = voucherRepo.findById(idVoucher)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phieu giam gia"));
        hd.setPhieuGiamGia(voucher);
        return recalculateInvoice(hd);
    }

    @Override
    public HoaDon thanhToan(Integer idHoaDon, String hinhThucThanhToan) {
        HoaDon hd = findById(idHoaDon);
        String trangThaiCu = hd.getTrangThai();
        hd.setTrangThai("Đã thanh toán");
        hd.setNgayThanhToan(LocalDateTime.now());
        hd.setNgayCapNhat(LocalDateTime.now());
        HoaDon saved = hoaDonRepo.save(hd);

        String tenPttt = hinhThucThanhToan == null || hinhThucThanhToan.isBlank()
                ? "Tiền mặt"
                : hinhThucThanhToan.trim();
        String maPttt = normalizePaymentCode(tenPttt);
        tenPttt = paymentDisplayName(maPttt, tenPttt);
        PhuongThucThanhToan pttt = findOrCreatePaymentMethod(maPttt, tenPttt);
        LocalDateTime paidAt = LocalDateTime.now();
        String transactionCode = maPttt + saved.getId() + System.currentTimeMillis();
        ThanhToan thanhToan = ThanhToan.builder()
                .hoaDon(saved)
                .phuongThucThanhToan(pttt)
                .maGiaoDich(transactionCode)
                .soTien(MoneyRoundingUtil.roundNonNegative(saved.getTongTienThanhToan()))
                .trangThai("Thành công")
                .thoiGianThanhToan(paidAt)
                .build();
        thanhToanRepo.save(thanhToan);

        LichSuThanhToan ls = LichSuThanhToan.builder()
                .hoaDon(saved)
                .maGiaoDich(transactionCode)
                .soTien(MoneyRoundingUtil.roundNonNegative(saved.getTongTienThanhToan()))
                .ngayThanhToan(paidAt)
                .hinhThucThanhToan(tenPttt)
                .loaiThanhToan("Thanh toán hóa đơn")
                .trangThai("Thành công")
                .build();
        lichSuRepo.save(ls);

        notifyStatusChanged(saved, trangThaiCu, saved.getTrangThai());
        return saved;
    }

    @Override public HoaDon findById(Integer id) { return hoaDonRepo.findWithRelationsById(id).orElseThrow(); }
    private HoaDon findByIdForUpdate(Integer id) {
        return hoaDonRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
    }
    @Override public List<HoaDonChiTietDTO> getChiTiet(Integer idHoaDon) { return chiTietRepo.findChiTietByHoaDonId(idHoaDon); }
    @Override
    public List<LichSuThanhToan> getLichSu(Integer idHoaDon) {
        List<ThanhToan> payments = thanhToanRepo.findByHoaDon_Id(idHoaDon);
        if (!payments.isEmpty()) {
            return payments.stream()
                    .map(this::toPaymentHistory)
                    .sorted(Comparator.comparing(
                            LichSuThanhToan::getNgayThanhToan,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .toList();
        }
        return lichSuRepo.findByHoaDon_IdOrderByNgayThanhToanDesc(idHoaDon);
    }

    private LichSuThanhToan toPaymentHistory(ThanhToan payment) {
        PhuongThucThanhToan method = payment.getPhuongThucThanhToan();
        String methodName = method == null ? null : method.getTenPttt();
        String methodCode = method == null ? null : method.getMaPttt();
        return LichSuThanhToan.builder()
                .maGiaoDich(payment.getMaGiaoDich())
                .soTien(MoneyRoundingUtil.roundNonNegative(payment.getSoTien()))
                .ngayThanhToan(payment.getThoiGianThanhToan())
                .hinhThucThanhToan(methodName == null || methodName.isBlank() ? methodCode : methodName)
                .loaiThanhToan("Thanh toán hóa đơn")
                .trangThai(payment.getTrangThai())
                .build();
    }

    private String normalizePaymentCode(String value) {
        String normalized = normalizeStatus(value).replace("-", "_").replace(" ", "_");
        if (normalized.contains("chuyen_khoan") || normalized.contains("bank") || normalized.contains("transfer")
                || normalized.equals("ck")) {
            return "CHUYEN_KHOAN";
        }
        if (normalized.contains("tien_mat") || normalized.equals("cash") || normalized.equals("tm")) {
            return "TIEN_MAT";
        }
        if (normalized.contains("mixed") || normalized.contains("ket_hop")) {
            return "KET_HOP";
        }
        if (normalized.contains("online") || normalized.contains("vnpay") || normalized.contains("momo")) {
            return "ONLINE";
        }
        if (normalized.contains("cod") || normalized.contains("nhan_hang")) {
            return "COD";
        }
        return value == null || value.isBlank() ? "COD" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String paymentDisplayName(String methodCode, String fallback) {
        return switch (normalizePaymentCode(methodCode)) {
            case "CHUYEN_KHOAN" -> "Chuyển khoản";
            case "TIEN_MAT" -> "Tiền mặt";
            case "KET_HOP" -> "Tiền mặt + Chuyển khoản";
            case "ONLINE" -> "Thanh toán online";
            case "COD" -> "Thanh toán khi nhận hàng";
            default -> fallback == null || fallback.isBlank() ? methodCode : fallback.trim();
        };
    }

    private PhuongThucThanhToan findOrCreatePaymentMethod(String code, String name) {
        String safeCode = code == null || code.isBlank() ? "COD" : code.trim();
        String safeName = name == null || name.isBlank() ? paymentDisplayName(safeCode, safeCode) : name.trim();
        return ptttRepo.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase(safeCode, safeName)
                .map(method -> {
                    if (!safeName.equals(method.getTenPttt())) {
                        method.setTenPttt(safeName);
                        return ptttRepo.save(method);
                    }
                    return method;
                })
                .orElseGet(() -> ptttRepo.save(PhuongThucThanhToan.builder()
                        .maPttt(safeCode)
                        .tenPttt(safeName)
                        .trangThai(true)
                        .build()));
    }

    @Override
    @Transactional
    public boolean updateThongTinKhachHang(Integer id, String ten, String sdt) {
        HoaDon hd = findById(id);
        hd.setTenKhachHang(ten == null ? null : ten.trim());
        hd.setSoDienThoai(sdt == null ? null : sdt.trim());
        hd.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepo.save(hd);
        return true;
    }

    private HoaDon recalculateInvoice(HoaDon hd) {
        BigDecimal subtotal = chiTietRepo.findByHoaDon_Id(hd.getId()).stream()
                .map(HoaDonChiTiet::getThanhTien)
                .map(MoneyRoundingUtil::roundNonNegative)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = discountAmount(hd.getPhieuGiamGia(), subtotal);
        BigDecimal shippingFee = MoneyRoundingUtil.roundNonNegative(hd.getPhiVanChuyen());
        hd.setTongTienGoc(subtotal);
        hd.setSoTienGiam(discount);
        hd.setPhiVanChuyen(shippingFee);
        hd.setTongTienThanhToan(MoneyRoundingUtil.roundNonNegative(subtotal.subtract(discount).add(shippingFee)));
        hd.setNgayCapNhat(LocalDateTime.now());
        return hoaDonRepo.save(hd);
    }

    private BigDecimal discountAmount(PhieuGiamGia voucher, BigDecimal subtotal) {
        if (voucher == null) return BigDecimal.ZERO;
        BigDecimal condition = MoneyRoundingUtil.roundNonNegative(voucher.getDieuKienDonHang());
        if (subtotal.compareTo(condition) < 0) return BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getTrangThai())
                || (voucher.getNgayBatDau() != null && voucher.getNgayBatDau().isAfter(now))
                || (voucher.getNgayKetThuc() != null && voucher.getNgayKetThuc().isBefore(now))) {
            return BigDecimal.ZERO;
        }
        int quantity = voucher.getSoLuong() == null ? Integer.MAX_VALUE : voucher.getSoLuong();
        int used = voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung();
        if (used >= quantity) return BigDecimal.ZERO;

        String type = normalizeStatus(voucher.getLoaiGiam());
        boolean percentDiscount = type.contains("phan") || type.contains("%");
        BigDecimal value = percentDiscount
                ? money(voucher.getGiaTri())
                : MoneyRoundingUtil.roundNonNegative(voucher.getGiaTri());
        BigDecimal discount = percentDiscount
                ? subtotal.multiply(value).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP)
                : value;
        BigDecimal max = MoneyRoundingUtil.roundNonNegative(voucher.getGiaTriToiDa());
        if (max.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(max) > 0) {
            discount = max;
        }
        return MoneyRoundingUtil.roundNonNegative(discount.min(subtotal));
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateInvoiceCode() {
        String code;
        do {
            code = "HD" + System.currentTimeMillis();
        } while (hoaDonRepo.existsByMaHoaDon(code));
        return code;
    }
}
