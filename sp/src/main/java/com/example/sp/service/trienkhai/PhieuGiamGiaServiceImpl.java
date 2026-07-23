package com.example.sp.service.trienkhai;

import com.example.sp.dto.khuyenmai.PhieuGiamGiaRequest;
import com.example.sp.model.khuyenmai.KhachHangPgg;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import com.example.sp.repository.khuyenmai.KhachHangPggRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.khuyenmai.PhieuGiamGiaRepository;
import com.example.sp.service.khuyenmai.PhieuGiamGiaService;
import com.example.sp.service.tienich.GeneratedCodeUtil;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaServiceImpl implements PhieuGiamGiaService {

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final KhachHangPggRepository khachHangPggRepository;
    private final KhachHangRepository khachHangRepository;

    @Override
    public Page<PhieuGiamGia> getAll(
            String keyword,
            String loaiGiam,
            Boolean trangThai,
            String tienDo,
            LocalDateTime tuNgay,
            LocalDateTime denNgay,
            Pageable pageable
    ) {
        return phieuGiamGiaRepository.search(
                blankToNull(keyword),
                blankToNull(loaiGiam),
                trangThai,
                blankToNull(tienDo),
                LocalDateTime.now(),
                tuNgay,
                denNgay,
                withDefaultSort(pageable)
        );
    }

    @Override
    public PhieuGiamGia findById(Integer id) {
        return phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá"));
    }

    @Override
    @Transactional
    public PhieuGiamGia save(PhieuGiamGiaRequest request) {
        validate(request);
        validateStrict(request);

        boolean creating = request.getId() == null;

        PhieuGiamGia pgg = creating
                ? new PhieuGiamGia()
                : findById(request.getId());
        validateStartTimeNotInPast(request.getNgayBatDau(), pgg.getNgayBatDau());
        validateEndTimeNotInPast(request.getNgayKetThuc());

        Integer idCheck = creating ? 0 : request.getId();

        String ma = blankToNull(request.getMaPgg());

        if (ma == null) {
            ma = generateCode(request.getTenPgg());
        }

        ma = ma.trim().toUpperCase();

        if (phieuGiamGiaRepository.existsByMaPggAndIdNot(ma, idCheck)) {
            throw new RuntimeException("Mã phiếu giảm giá đã tồn tại");
        }

        pgg.setMaPgg(ma);
        pgg.setTenPgg(request.getTenPgg().trim());
        pgg.setLoaiGiam(request.getLoaiGiam());
        boolean percentVoucher = "PHAN_TRAM".equalsIgnoreCase(request.getLoaiGiam());
        pgg.setGiaTri(percentVoucher
                ? request.getGiaTri()
                : MoneyRoundingUtil.roundNonNegative(request.getGiaTri()));
        pgg.setGiaTriToiDa(request.getGiaTriToiDa() == null
                ? null
                : MoneyRoundingUtil.roundNonNegative(request.getGiaTriToiDa()));
        pgg.setDieuKienDonHang(request.getDieuKienDonHang() == null
                ? null
                : MoneyRoundingUtil.roundNonNegative(request.getDieuKienDonHang()));
        pgg.setNgayBatDau(request.getNgayBatDau());
        pgg.setNgayKetThuc(request.getNgayKetThuc());
        pgg.setSoLuong(request.getSoLuong());

        pgg.setSoLuongDaDung(
                request.getSoLuongDaDung() == null
                        ? safeUsed(pgg)
                        : request.getSoLuongDaDung()
        );

        /*
         * QUAN TRỌNG:
         * - Ngày kết thúc > hiện tại => tự bật phiếu.
         * - Ngày kết thúc <= hiện tại => tự tắt phiếu.
         *
         * Áp dụng cho cả thêm mới và cập nhật.
         */
        pgg.setTrangThai(isVoucherStillActive(request.getNgayKetThuc()));

        PhieuGiamGia saved = phieuGiamGiaRepository.save(pgg);

        replaceKhachHangLinks(saved.getId(), request.getKhachHangIds());

        return saved;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        phieuGiamGiaRepository.softDeleteById(id);
    }

    @Override
    @Transactional
    public PhieuGiamGia toggleStatus(Integer id) {
        PhieuGiamGia voucher = findById(id);
        LocalDateTime now = LocalDateTime.now();

        /*
         * Nếu đang bật => cho phép tắt thủ công.
         */
        if (Boolean.TRUE.equals(voucher.getTrangThai())) {
            voucher.setTrangThai(false);
            return phieuGiamGiaRepository.save(voucher);
        }

        /*
         * Nếu đang tắt nhưng ngày kết thúc đã qua
         * thì không cho bật lại khi chưa sửa ngày kết thúc.
         */
        if (voucher.getNgayKetThuc() == null
                || !voucher.getNgayKetThuc().isAfter(now)) {
            throw new RuntimeException(
                    "Phiếu giảm giá đã kết thúc. "
                            + "Hãy cập nhật ngày kết thúc lớn hơn thời điểm hiện tại trước khi bật lại."
            );
        }

        /*
         * Còn hạn thì bật lại.
         * Không tự sửa ngày bắt đầu và ngày kết thúc.
         */
        voucher.setTrangThai(true);

        return phieuGiamGiaRepository.save(voucher);
    }

    @Override
    public List<Integer> getKhachHangIds(Integer idPgg) {
        try {
            return khachHangPggRepository.findIdKhByIdPgg(idPgg);
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    @Override
    public boolean validateVoucher(Integer idVoucher, Double tongTien) {
        if (idVoucher == null || tongTien == null) {
            return false;
        }

        PhieuGiamGia voucher = findById(idVoucher);
        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(voucher.getTrangThai())) {
            return false;
        }

        if (voucher.getNgayBatDau() != null
                && now.isBefore(voucher.getNgayBatDau())) {
            return false;
        }

        if (voucher.getNgayKetThuc() != null
                && !voucher.getNgayKetThuc().isAfter(now)) {
            return false;
        }

        if (voucher.getSoLuong() != null
                && voucher.getSoLuongDaDung() != null
                && voucher.getSoLuongDaDung() >= voucher.getSoLuong()) {
            return false;
        }

        return voucher.getDieuKienDonHang() == null
                || BigDecimal.valueOf(tongTien)
                .compareTo(voucher.getDieuKienDonHang()) >= 0;
    }

    private void replaceKhachHangLinks(
            Integer idPgg,
            List<Integer> khachHangIds
    ) {
        try {
            khachHangPggRepository.deleteByIdPgg(idPgg);
        } catch (DataAccessException ex) {
            if (khachHangIds == null || khachHangIds.isEmpty()) {
                return;
            }

            throw new RuntimeException(
                    "Bảng khach_hang_pgg chưa tồn tại nên chưa thể gán voucher cho khách hàng cụ thể."
            );
        }

        if (khachHangIds == null || khachHangIds.isEmpty()) {
            return;
        }

        try {
            khachHangIds.stream()
                    .distinct()
                    .forEach(idKh -> {
                        KhachHangPgg link = new KhachHangPgg();
                        link.setIdPgg(idPgg);
                        link.setIdKh(idKh);

                        khachHangPggRepository.save(link);
                    });
        } catch (DataAccessException ex) {
            throw new RuntimeException(
                    "Bảng khach_hang_pgg chưa tồn tại nên chưa thể gán voucher cho khách hàng cụ thể."
            );
        }
    }

    private Pageable withDefaultSort(Pageable pageable) {
        Sort sort = Sort.by(Sort.Order.desc("id"));

        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged(sort);
        }

        if (pageable.getSort().isSorted()) {
            return pageable;
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }

    private void validate(PhieuGiamGiaRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu phiếu giảm giá không hợp lệ");
        }

        if (blankToNull(request.getTenPgg()) == null) {
            throw new RuntimeException("Tên phiếu giảm giá không được để trống");
        }

        if (blankToNull(request.getLoaiGiam()) == null) {
            throw new RuntimeException("Vui lòng chọn loại giảm");
        }

        if (request.getGiaTri() == null
                || request.getGiaTri().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá trị giảm phải lớn hơn 0");
        }

        if ("PHAN_TRAM".equals(request.getLoaiGiam())
                && request.getGiaTri().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException(
                    "Giá trị giảm phần trăm không được vượt quá 100"
            );
        }

        if (request.getSoLuong() == null || request.getSoLuong() < 0) {
            throw new RuntimeException("Số lượng không hợp lệ");
        }

        if (request.getNgayBatDau() == null
                || request.getNgayKetThuc() == null) {
            throw new RuntimeException(
                    "Vui lòng nhập thời gian bắt đầu và kết thúc"
            );
        }

        if (!request.getNgayKetThuc().isAfter(request.getNgayBatDau())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private void validateStrict(PhieuGiamGiaRequest request) {
        String ma = blankToNull(request.getMaPgg());

        if (ma != null && !ma.matches("^[A-Za-z0-9_-]{3,30}$")) {
            throw new RuntimeException(
                    "Mã phiếu chỉ được gồm chữ, số, gạch ngang/gạch dưới và dài 3-30 ký tự"
            );
        }

        String ten = blankToNull(request.getTenPgg());

        if (ten == null || ten.length() < 3 || ten.length() > 100) {
            throw new RuntimeException("Tên phiếu phải từ 3 đến 100 ký tự");
        }

        String loaiGiam = blankToNull(request.getLoaiGiam());

        if (!"PHAN_TRAM".equals(loaiGiam)
                && !"TIEN_MAT".equals(loaiGiam)) {
            throw new RuntimeException("Loại giảm không hợp lệ");
        }

        if ("TIEN_MAT".equals(loaiGiam)
                && request.getGiaTri().scale() > 0) {
            throw new RuntimeException(
                    "Giá trị giảm tiền mặt phải là số nguyên"
            );
        }

        if (request.getGiaTriToiDa() != null
                && request.getGiaTriToiDa().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Giá trị tối đa không được âm");
        }

        if (request.getGiaTriToiDa() != null
                && request.getGiaTriToiDa().scale() > 0) {
            throw new RuntimeException("Giá trị tối đa phải là số nguyên");
        }

        if ("TIEN_MAT".equals(loaiGiam)
                && request.getGiaTriToiDa() != null
                && request.getGiaTriToiDa().compareTo(request.getGiaTri()) > 0) {
            throw new RuntimeException(
                    "Giá trị tối đa không được lớn hơn giá trị giảm tiền mặt"
            );
        }

        if (request.getDieuKienDonHang() != null
                && request.getDieuKienDonHang().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Điều kiện đơn hàng không được âm");
        }

        if (request.getDieuKienDonHang() != null
                && request.getDieuKienDonHang().scale() > 0) {
            throw new RuntimeException(
                    "Điều kiện đơn hàng phải là số nguyên"
            );
        }

        if (request.getDieuKienDonHang() != null
                && "TIEN_MAT".equals(loaiGiam)
                && request.getGiaTri().compareTo(request.getDieuKienDonHang()) > 0) {
            throw new RuntimeException(
                    "Giá trị giảm tiền mặt không được lớn hơn điều kiện đơn hàng"
            );
        }

        if (request.getSoLuong() == null || request.getSoLuong() <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        if (request.getSoLuong() > 1_000_000) {
            throw new RuntimeException(
                    "Số lượng không được vượt quá 1.000.000"
            );
        }

        if (request.getSoLuongDaDung() != null
                && request.getSoLuongDaDung() < 0) {
            throw new RuntimeException("Số lượng đã dùng không được âm");
        }

        if (request.getSoLuongDaDung() != null
                && request.getSoLuongDaDung() > request.getSoLuong()) {
            throw new RuntimeException(
                    "Số lượng đã dùng không được lớn hơn số lượng"
            );
        }

        if (request.getKhachHangIds() != null) {
            List<Integer> ids = request.getKhachHangIds()
                    .stream()
                    .distinct()
                    .toList();

            if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new RuntimeException(
                        "Danh sách khách hàng áp dụng không hợp lệ"
                );
            }

            if (ids.stream().anyMatch(id -> !khachHangRepository.existsById(id))) {
                throw new RuntimeException(
                        "Có khách hàng áp dụng không tồn tại"
                );
            }
        }
    }

    private boolean isVoucherStillActive(LocalDateTime ngayKetThuc) {
        return ngayKetThuc != null
                && ngayKetThuc.isAfter(LocalDateTime.now());
    }

    private Integer safeUsed(PhieuGiamGia pgg) {
        return pgg.getSoLuongDaDung() == null
                ? 0
                : pgg.getSoLuongDaDung();
    }

    private String generateCode(String name) {
        return GeneratedCodeUtil.fromNameAndDate(
                name,
                null,
                "PGG",
                code -> phieuGiamGiaRepository.existsByMaPggAndIdNot(code, 0)
        );
    }

    private void validateStartTimeNotInPast(LocalDateTime requestedStart, LocalDateTime currentStart) {
        if (requestedStart == null) return;
        if (currentStart != null && currentStart.equals(requestedStart)) return;
        if (requestedStart.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new RuntimeException("Ngay bat dau khong duoc la thoi gian trong qua khu");
        }
    }

    private void validateEndTimeNotInPast(LocalDateTime requestedEnd) {
        if (requestedEnd == null) return;
        if (requestedEnd.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new RuntimeException("Ngay ket thuc khong duoc la thoi gian trong qua khu");
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
