package com.example.sp.controller.khuyenmai;

import com.example.sp.dto.khuyenmai.PhieuGiamGiaRequest;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import com.example.sp.service.khuyenmai.PhieuGiamGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/phieu-giam-gia")
@RequiredArgsConstructor
public class PhieuGiamGiaController {

    private final PhieuGiamGiaService phieuGiamGiaService;

    @GetMapping
    // Tải hoặc truy xuất dữ liệu cho get all.
    public Page<PhieuGiamGia> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loaiGiam,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) String tienDo,
            @RequestParam(required = false) LocalDateTime tuNgay,
            @RequestParam(required = false) LocalDateTime denNgay,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return phieuGiamGiaService.getAll(keyword, loaiGiam, trangThai, tienDo, tuNgay, denNgay, pageable);
    }

    @GetMapping("/{id}")
    // Tải hoặc truy xuất dữ liệu cho get by id.
    public PhieuGiamGia getById(@PathVariable Integer id) {
        return phieuGiamGiaService.findById(id);
    }

    @PostMapping
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create.
    public PhieuGiamGia create(@RequestBody PhieuGiamGiaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu phiếu giảm giá không hợp lệ");
        }
        request.setId(null);
        return phieuGiamGiaService.save(request);
    }

    @PutMapping("/{id}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update.
    public PhieuGiamGia update(@PathVariable Integer id, @RequestBody PhieuGiamGiaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu phiếu giảm giá không hợp lệ");
        }
        request.setId(id);
        return phieuGiamGiaService.save(request);
    }

    @PatchMapping("/{id}/trang-thai")
    // Xử lý tương tác người dùng cho toggle status.
    public PhieuGiamGia toggleStatus(@PathVariable Integer id) {
        return phieuGiamGiaService.toggleStatus(id);
    }

    @DeleteMapping("/{id}")
    // Xử lý thao tác đóng, xóa hoặc hủy cho delete.
    public void delete(@PathVariable Integer id) {
        phieuGiamGiaService.delete(id);
    }

    // Thực hiện xử lý nghiệp vụ của hàm to request.
    private PhieuGiamGiaRequest toRequest(PhieuGiamGia voucher) {
        PhieuGiamGiaRequest request = new PhieuGiamGiaRequest();
        request.setId(voucher.getId());
        request.setMaPgg(voucher.getMaPgg());
        request.setTenPgg(voucher.getTenPgg());
        request.setLoaiGiam(voucher.getLoaiGiam());
        request.setGiaTri(voucher.getGiaTri());
        request.setGiaTriToiDa(voucher.getGiaTriToiDa());
        request.setDieuKienDonHang(voucher.getDieuKienDonHang());
        request.setNgayBatDau(voucher.getNgayBatDau());
        request.setNgayKetThuc(voucher.getNgayKetThuc());
        request.setSoLuong(voucher.getSoLuong());
        request.setSoLuongDaDung(voucher.getSoLuongDaDung());
        request.setTrangThai(voucher.getTrangThai());
        return request;
    }
}
