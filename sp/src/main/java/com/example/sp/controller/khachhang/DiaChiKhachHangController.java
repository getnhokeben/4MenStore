package com.example.sp.controller.khachhang;

import com.example.sp.dto.khachhang.DiaChiKhachHangDTO;
import com.example.sp.dto.khachhang.DiaChiKhachHangRequest;
import com.example.sp.service.khachhang.DiaChiKhachHangService;
import com.example.sp.service.cuahang.ShopSessionKeys;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/khach-hang/{customerId}/dia-chi")
@RequiredArgsConstructor
public class DiaChiKhachHangController {

    private final DiaChiKhachHangService diaChiKhachHangService;

    @GetMapping
    // Tải hoặc truy xuất dữ liệu cho find by customer.
    public List<DiaChiKhachHangDTO> findByCustomer(
            @PathVariable Integer customerId,
            HttpSession session
    ) {
        assertCanAccess(customerId, session);
        return diaChiKhachHangService.findByCustomer(customerId);
    }

    @PostMapping
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create.
    public ResponseEntity<DiaChiKhachHangDTO> create(
            @PathVariable Integer customerId,
            @Valid @RequestBody DiaChiKhachHangRequest request,
            HttpSession session
    ) {
        assertCanAccess(customerId, session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diaChiKhachHangService.create(customerId, request));
    }

    @PutMapping("/{addressId}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update.
    public DiaChiKhachHangDTO update(
            @PathVariable Integer customerId,
            @PathVariable Integer addressId,
            @Valid @RequestBody DiaChiKhachHangRequest request,
            HttpSession session
    ) {
        assertCanAccess(customerId, session);
        return diaChiKhachHangService.update(
                customerId,
                addressId,
                request
        );
    }

    @PatchMapping("/{addressId}/mac-dinh")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho set default.
    public DiaChiKhachHangDTO setDefault(
            @PathVariable Integer customerId,
            @PathVariable Integer addressId,
            HttpSession session
    ) {
        assertCanAccess(customerId, session);
        return diaChiKhachHangService.setDefault(
                customerId,
                addressId
        );
    }

    @DeleteMapping("/{addressId}")
    // Xử lý thao tác đóng, xóa hoặc hủy cho delete.
    public ResponseEntity<Void> delete(
            @PathVariable Integer customerId,
            @PathVariable Integer addressId,
            HttpSession session
    ) {
        assertCanAccess(customerId, session);
        diaChiKhachHangService.delete(customerId, addressId);

        return ResponseEntity.noContent().build();
    }

    // Thực hiện xử lý nghiệp vụ của hàm assert can access.
    private void assertCanAccess(Integer customerId, HttpSession session) {
        if (session != null && session.getAttribute(KhoaSessionNhanVien.NHANVIEN_ID) != null) {
            return;
        }

        Integer loggedInCustomerId = session == null ? null
                : (Integer) session.getAttribute(ShopSessionKeys.CUSTOMER_ID);
        if (customerId != null && customerId.equals(loggedInCustomerId)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền truy cập địa chỉ này");
    }
}
