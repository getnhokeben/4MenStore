package com.example.sp.controller.sanpham;

import com.example.sp.dto.cuahang.ShopReviewDTO;
import com.example.sp.service.cuahang.ShopReviewService;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PhanHoiSanPhamController {

    private final ShopReviewService shopReviewService;

    @GetMapping("/api/admin/phan-hoi")
    // Thực hiện xử lý nghiệp vụ của hàm all.
    public List<ShopReviewDTO> all(HttpSession session) {
        requireEmployee(session);
        return shopReviewService.getAllForAdmin();
    }

    @PatchMapping("/api/admin/phan-hoi/{reviewId}/trang-thai")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update status.
    public ResponseEntity<ShopReviewDTO> updateStatus(@PathVariable Integer reviewId,
                                                       @RequestBody Map<String, Boolean> body,
                                                       HttpSession session) {
        requireEmployee(session);
        Boolean visible = body == null ? null : body.get("trangThai");
        if (visible == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu trạng thái hiển thị");
        }
        return ResponseEntity.ok(shopReviewService.updateVisibility(reviewId, visible));
    }

    @PatchMapping("/api/admin/phan-hoi/{reviewId}/tra-loi")
    // Thực hiện xử lý nghiệp vụ của hàm reply.
    public ResponseEntity<ShopReviewDTO> reply(@PathVariable Integer reviewId,
                                                @RequestBody Map<String, String> body,
                                                HttpSession session) {
        requireEmployee(session);
        String content = body == null ? null : body.get("noiDung");
        return ResponseEntity.ok(shopReviewService.replyToReview(reviewId, content));
    }

    // Thực hiện xử lý nghiệp vụ của hàm require employee.
    private void requireEmployee(HttpSession session) {
        if (session == null || session.getAttribute(KhoaSessionNhanVien.NHANVIEN_ID) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập tài khoản nhân viên");
        }
    }
}
