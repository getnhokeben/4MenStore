package com.example.sp.service.trienkhai;

import com.example.sp.dto.khuyenmai.DotGiamGiaRequest;
import com.example.sp.dto.khuyenmai.SanPhamChiTietPromotionView;
import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.model.khuyenmai.ChiTietDotGiamGia;
import com.example.sp.model.khuyenmai.DotGiamGia;
import com.example.sp.repository.khuyenmai.ChiTietDotGiamGiaRepository;
import com.example.sp.repository.sanpham.ChiTietSanPhamRepository;
import com.example.sp.repository.khuyenmai.DotGiamGiaRepository;
import com.example.sp.service.tienich.GeneratedCodeUtil;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.service.khuyenmai.DotGiamGiaService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
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
public class DotGiamGiaServiceImpl implements DotGiamGiaService {

    private final DotGiamGiaRepository dotGiamGiaRepository;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    private final ChiTietSanPhamRepository chiTietSanPhamRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public List<DotGiamGia> getAll() {
        deactivateExpiredPromotions();
        return dotGiamGiaRepository.findAll(Sort.by(Sort.Order.desc("id")));
    }

    @Override
    @Transactional
    public Page<DotGiamGia> getAll(String keyword, Boolean trangThai, String tienDo, LocalDateTime tuNgay, LocalDateTime denNgay, Pageable pageable) {
        deactivateExpiredPromotions();
        return dotGiamGiaRepository.search(blankToNull(keyword), trangThai, blankToNull(tienDo), LocalDateTime.now(), tuNgay, denNgay, withDefaultSort(pageable));
    }

    @Override
    @Transactional
    public DotGiamGia findById(Integer id) {
        deactivateExpiredPromotions();
        return dotGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt giảm giá"));
    }

    private void deactivateExpiredPromotions() {
        dotGiamGiaRepository.deactivateExpired(LocalDateTime.now());
    }

    @Override
    @Transactional
    public DotGiamGia save(DotGiamGia dg) {
        DotGiamGiaRequest request = new DotGiamGiaRequest();
        request.setId(dg.getId());
        request.setMaDotGiamGia(dg.getMaDotGiamGia());
        request.setTenDotGiamGia(dg.getTenDotGiamGia());
        request.setLoaiGiamGia(dg.getLoaiGiamGia());
        request.setGiaTriGiamGia(dg.getGiaTriGiamGia());
        request.setSoTienToiDa(dg.getSoTienToiDa());
        request.setNgayBatDau(dg.getNgayBatDau());
        request.setNgayKetThuc(dg.getNgayKetThuc());
        request.setTrangThai(dg.getTrangThai());
        return save(request);
    }

    @Override
    @Transactional
    public DotGiamGia save(DotGiamGiaRequest request) {
        validate(request);
        validateStrict(request);

        DotGiamGia dot = request.getId() == null ? new DotGiamGia() : findById(request.getId());
        validateStartTimeNotInPast(request.getNgayBatDau(), dot.getNgayBatDau());
        validateEndTimeNotInPast(request.getNgayKetThuc());
        Integer idCheck = request.getId() == null ? 0 : request.getId();

        String ma = blankToNull(request.getMaDotGiamGia());
        if (ma == null) {
            ma = generateCode(request.getTenDotGiamGia());
        }
        ma = ma.trim().toUpperCase();
        if (dotGiamGiaRepository.existsByMaDotGiamGiaAndIdNot(ma, idCheck)) {
            throw new RuntimeException("Mã đợt giảm giá đã tồn tại");
        }

        dot.setMaDotGiamGia(ma);
        dot.setTenDotGiamGia(request.getTenDotGiamGia().trim());
        String discountType = blankToNull(request.getLoaiGiamGia()) == null ? "PHAN_TRAM" : request.getLoaiGiamGia();
        dot.setLoaiGiamGia(discountType);
        dot.setGiaTriGiamGia("PHAN_TRAM".equalsIgnoreCase(discountType)
                ? request.getGiaTriGiamGia()
                : MoneyRoundingUtil.roundNonNegative(request.getGiaTriGiamGia()));
        dot.setSoTienToiDa(request.getSoTienToiDa() == null
                ? null
                : MoneyRoundingUtil.roundNonNegative(request.getSoTienToiDa()));
        dot.setNgayBatDau(request.getNgayBatDau());
        dot.setNgayKetThuc(request.getNgayKetThuc());
        dot.setTrangThai(request.getTrangThai() == null ? Boolean.TRUE : request.getTrangThai());

        DotGiamGia saved = dotGiamGiaRepository.save(dot);
        replaceSanPhamLinks(saved, request.getSelectedSpctIds());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        DotGiamGia dot = findById(id);
        dot.setTrangThai(false);
        dotGiamGiaRepository.save(dot);
    }

    @Override
    @Transactional
    public DotGiamGia toggleStatus(Integer id) {
        DotGiamGia dot = findById(id);
        return setStatus(id, !Boolean.TRUE.equals(dot.getTrangThai()));
    }

    @Override
    @Transactional
    public DotGiamGia setStatus(Integer id, Boolean trangThai) {
        if (trangThai == null) {
            return toggleStatus(id);
        }
        DotGiamGia dot = findById(id);
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(trangThai)) {
            if (dot.getNgayKetThuc() == null || !dot.getNgayKetThuc().isAfter(now)) {
                throw new RuntimeException("Dot giam gia da ket thuc. Hay cap nhat ngay ket thuc lon hon thoi diem hien tai truoc khi bat lai.");
            }
            dotGiamGiaRepository.updateTrangThaiById(id, true);
        } else {
            dotGiamGiaRepository.updateTrangThaiById(id, false);
        }
        return findById(id);
    }

    @Override
    public List<Integer> getSelectedSpctIds(Integer idDotGiamGia) {
        return chiTietDotGiamGiaRepository.findIdSpctByDotGiamGiaId(idDotGiamGia);
    }

    @Override
    public List<SanPhamChiTietPromotionView> getSanPhamChiTietKichHoat() {
        return dotGiamGiaRepository.findSanPhamChiTietKichHoat();
    }

    private void replaceSanPhamLinks(DotGiamGia dot, List<Integer> selectedSpctIds) {
        chiTietDotGiamGiaRepository.deleteByDotGiamGiaId(dot.getId());
        if (selectedSpctIds == null || selectedSpctIds.isEmpty()) {
            return;
        }
        selectedSpctIds.stream()
                .distinct()
                .forEach(idSpct -> {
                    ChiTietDotGiamGia detail = new ChiTietDotGiamGia();
                    detail.setDotGiamGia(dot);
                    detail.setChiTietSanPham(entityManager.getReference(ChiTietSanPham.class, idSpct));
                    detail.setTrangThai(true);
                    chiTietDotGiamGiaRepository.save(detail);
                });
    }

    private Pageable withDefaultSort(Pageable pageable) {
        Sort sort = Sort.by(Sort.Order.desc("id"));
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged(sort);
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private void validate(DotGiamGiaRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu đợt giảm giá không hợp lệ");
        }
        if (blankToNull(request.getTenDotGiamGia()) == null) {
            throw new RuntimeException("Tên đợt giảm giá không được để trống");
        }
        String ten = request.getTenDotGiamGia().trim();
        if (ten.length() < 3 || ten.length() > 100) {
            throw new RuntimeException("Tên đợt giảm giá phải dài từ 3 đến 100 ký tự");
        }
        if (request.getGiaTriGiamGia() == null || request.getGiaTriGiamGia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá trị giảm phải lớn hơn 0");
        }
        String loai = blankToNull(request.getLoaiGiamGia()) == null ? "PHAN_TRAM" : request.getLoaiGiamGia();
        if ("PHAN_TRAM".equals(loai) && request.getGiaTriGiamGia().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Giá trị giảm phần trăm không được vượt quá 100");
        }
        if (request.getNgayBatDau() == null || request.getNgayKetThuc() == null) {
            throw new RuntimeException("Vui lòng nhập thời gian bắt đầu và kết thúc");
        }
        if (!request.getNgayKetThuc().isAfter(request.getNgayBatDau())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private void validateStrict(DotGiamGiaRequest request) {
        String ma = blankToNull(request.getMaDotGiamGia());
        if (ma != null && !ma.matches("^[A-Za-z0-9_-]{3,30}$")) {
            throw new RuntimeException("Mã đợt giảm giá chỉ được gồm chữ, số, gạch ngang/gạch dưới và dài 3-30 ký tự");
        }

        String loaiGiam = blankToNull(request.getLoaiGiamGia()) == null ? "PHAN_TRAM" : blankToNull(request.getLoaiGiamGia());
        if (!"PHAN_TRAM".equals(loaiGiam) && !"TIEN_MAT".equals(loaiGiam)) {
            throw new RuntimeException("Loại giảm không hợp lệ");
        }
        if ("TIEN_MAT".equals(loaiGiam) && request.getGiaTriGiamGia().scale() > 0) {
            throw new RuntimeException("Giá trị giảm tiền mặt phải là số nguyên");
        }
        if (request.getSoTienToiDa() != null && request.getSoTienToiDa().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Số tiền tối đa không được âm");
        }
        if ("PHAN_TRAM".equals(loaiGiam) && request.getSoTienToiDa() == null) {
            throw new RuntimeException("Vui lòng nhập số tiền tối đa khi giảm theo phần trăm");
        }
        if ("TIEN_MAT".equals(loaiGiam) && request.getSoTienToiDa() != null && request.getSoTienToiDa().compareTo(request.getGiaTriGiamGia()) > 0) {
            throw new RuntimeException("Số tiền tối đa không được lớn hơn giá trị giảm tiền mặt");
        }
        if (request.getSelectedSpctIds() == null || request.getSelectedSpctIds().isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm chi tiết áp dụng");
        }
        List<Integer> ids = request.getSelectedSpctIds().stream().distinct().toList();
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new RuntimeException("Danh sách sản phẩm chi tiết áp dụng không hợp lệ");
        }
        if (ids.stream().anyMatch(id -> !chiTietSanPhamRepository.existsActiveVariantById(id))) {
            throw new RuntimeException("Có sản phẩm chi tiết áp dụng không tồn tại hoặc đã ngừng hoạt động");
        }
    }

    private String generateCode(String name) {
        return GeneratedCodeUtil.fromNameAndDate(
                name,
                null,
                "DGG",
                code -> dotGiamGiaRepository.existsByMaDotGiamGiaAndIdNot(code, 0)
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
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

