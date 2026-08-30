package com.example.sp.service.cuahang;

import com.example.sp.dto.cuahang.ShopReviewDTO;
import com.example.sp.dto.cuahang.ShopReviewRequest;
import com.example.sp.dto.cuahang.ShopReviewSummaryDTO;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.model.sanpham.PhanHoiSanPham;
import com.example.sp.model.sanpham.SanPham;
import com.example.sp.repository.hoadon.HoaDonChiTietRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.sanpham.PhanHoiSanPhamRepository;
import com.example.sp.repository.sanpham.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopReviewService {

    private final PhanHoiSanPhamRepository reviewRepository;
    private final SanPhamRepository sanPhamRepository;
    private final KhachHangRepository khachHangRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    @Transactional(readOnly = true)
    // Tải hoặc truy xuất dữ liệu cho get visible reviews.
    public ShopReviewSummaryDTO getVisibleReviews(Integer productId) {
        ensureProduct(productId);
        List<PhanHoiSanPham> reviews = reviewRepository
                .findBySanPham_IdSpAndTrangThaiTrueOrderByNgayTaoDesc(productId);
        return summary(productId, reviews);
    }

    @Transactional
    // Xử lý tương tác người dùng cho submit.
    public ShopReviewDTO submit(Integer productId, Integer customerId, ShopReviewRequest request) {
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để đánh giá sản phẩm");
        }

        SanPham product = ensureProduct(productId);
        KhachHang customer = khachHangRepository.findById(customerId)
                .filter(item -> Boolean.TRUE.equals(item.getTrangThai()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không còn khả dụng"));

        if (!hoaDonChiTietRepository.hasCompletedOrderProduct(customerId, productId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn chỉ có thể đánh giá sản phẩm thuộc đơn hàng đã hoàn thành");
        }

        PhanHoiSanPham review = reviewRepository
                .findBySanPham_IdSpAndKhachHang_Id(productId, customerId)
                .orElseGet(PhanHoiSanPham::new);
        boolean creating = review.getId() == null;

        review.setSanPham(product);
        review.setKhachHang(customer);
        review.setDiemDanhGia(request.getDiemDanhGia());
        review.setNoiDung(normalizeContent(request.getNoiDung()));
        review.setTrangThai(true);
        if (creating) {
            review.setNgayTao(LocalDateTime.now());
        }
        review.markUpdated();

        return toDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    // Tải hoặc truy xuất dữ liệu cho get all for admin.
    public List<ShopReviewDTO> getAllForAdmin() {
        return reviewRepository.findAllByOrderByNgayTaoDesc().stream()
                .map(this::toAdminDto)
                .toList();
    }

    @Transactional(readOnly = true)
    // Tải hoặc truy xuất dữ liệu cho get my reviews.
    public List<ShopReviewDTO> getMyReviews(Integer customerId) {
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem đánh giá");
        }
        return reviewRepository.findByKhachHang_IdOrderByNgayCapNhatDesc(customerId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update visibility.
    public ShopReviewDTO updateVisibility(Integer reviewId, boolean visible) {
        PhanHoiSanPham review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá"));
        review.setTrangThai(visible);
        review.markUpdated();
        return toDto(reviewRepository.save(review));
    }

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm reply to review.
    public ShopReviewDTO replyToReview(Integer reviewId, String content) {
        String reply = normalizeContent(content);
        if (reply == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung phản hồi không được để trống");
        }

        PhanHoiSanPham review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá"));
        review.setPhanHoiQuanTri(reply);
        review.setNgayPhanHoi(LocalDateTime.now());
        review.markUpdated();
        return toAdminDto(reviewRepository.save(review));
    }

    // Thực hiện xử lý nghiệp vụ của hàm summary.
    private ShopReviewSummaryDTO summary(Integer productId, List<PhanHoiSanPham> reviews) {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            counts.put(star, 0L);
        }
        reviewRepository.countByRatingForProduct(productId).forEach(row -> {
            Integer star = ((Number) row[0]).intValue();
            counts.put(star, ((Number) row[1]).longValue());
        });

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        BigDecimal average = total == 0
                ? BigDecimal.ZERO
                : counts.entrySet().stream()
                .map(entry -> BigDecimal.valueOf((long) entry.getKey() * entry.getValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);

        return ShopReviewSummaryDTO.builder()
                .diemTrungBinh(average)
                .tongDanhGia(total)
                .thongKeSao(counts)
                .danhSach(reviews.stream().map(this::toDto).toList())
                .build();
    }

    // Thực hiện xử lý nghiệp vụ của hàm ensure product.
    private SanPham ensureProduct(Integer productId) {
        return sanPhamRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize content.
    private String normalizeContent(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    // Thực hiện xử lý nghiệp vụ của hàm to dto.
    private ShopReviewDTO toDto(PhanHoiSanPham review) {
        String name = review.getKhachHang().getTenKhachHang();
        return ShopReviewDTO.builder()
                .id(review.getId())
                .idSp(review.getSanPham().getIdSp())
                .tenSp(review.getSanPham().getTenSp())
                .hinhAnh(review.getSanPham().getHinhAnh())
                .tenKhachHang(maskName(name))
                .diemDanhGia(review.getDiemDanhGia())
                .noiDung(review.getNoiDung())
                .phanHoiQuanTri(review.getPhanHoiQuanTri())
                .trangThai(review.getTrangThai())
                .ngayTao(review.getNgayTao())
                .ngayPhanHoi(review.getNgayPhanHoi())
                .build();
    }

    // Thực hiện xử lý nghiệp vụ của hàm to admin dto.
    private ShopReviewDTO toAdminDto(PhanHoiSanPham review) {
        String name = review.getKhachHang().getTenKhachHang();
        return ShopReviewDTO.builder()
                .id(review.getId())
                .idSp(review.getSanPham().getIdSp())
                .tenSp(review.getSanPham().getTenSp())
                .hinhAnh(review.getSanPham().getHinhAnh())
                .tenKhachHang(name == null || name.isBlank() ? "Khách hàng" : name)
                .diemDanhGia(review.getDiemDanhGia())
                .noiDung(review.getNoiDung())
                .phanHoiQuanTri(review.getPhanHoiQuanTri())
                .trangThai(review.getTrangThai())
                .ngayTao(review.getNgayTao())
                .ngayPhanHoi(review.getNgayPhanHoi())
                .build();
    }

    // Thực hiện xử lý nghiệp vụ của hàm mask name.
    private String maskName(String name) {
        if (name == null || name.isBlank()) return "Khách hàng";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].charAt(0) + "***";
        }
        return parts[0].charAt(0) + "*** " + parts[parts.length - 1];
    }
}
