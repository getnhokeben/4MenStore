package com.example.sp.controller.khuyenmai;

import com.example.sp.dto.khuyenmai.DotGiamGiaRequest;
import com.example.sp.dto.khuyenmai.SanPhamChiTietPromotionView;
import com.example.sp.model.khuyenmai.DotGiamGia;
import com.example.sp.service.khuyenmai.DotGiamGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/dot-giam-gia")
@RequiredArgsConstructor
public class DotGiamGiaController {

    private final DotGiamGiaService dotGiamGiaService;

    @GetMapping
    // Tải hoặc truy xuất dữ liệu cho get all.
    public Page<DotGiamGia> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) String tienDo,
            @RequestParam(required = false) LocalDateTime tuNgay,
            @RequestParam(required = false) LocalDateTime denNgay,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return dotGiamGiaService.getAll(keyword, trangThai, tienDo, tuNgay, denNgay, pageable);
    }

    @GetMapping("/{id}")
    // Tải hoặc truy xuất dữ liệu cho get by id.
    public DotGiamGia getById(@PathVariable Integer id) {
        return dotGiamGiaService.findById(id);
    }

    @PostMapping
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create.
    public DotGiamGia create(@RequestBody DotGiamGiaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đợt giảm giá không hợp lệ");
        }
        request.setId(null);
        return dotGiamGiaService.save(request);
    }

    @PutMapping("/{id}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update.
    public DotGiamGia update(@PathVariable Integer id, @RequestBody DotGiamGiaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đợt giảm giá không hợp lệ");
        }
        request.setId(id);
        return dotGiamGiaService.save(request);
    }

    @PatchMapping({"/{id}/trang-thai", "/trang-thai/{id}"})
    // Xử lý tương tác người dùng cho toggle status.
    public DotGiamGia toggleStatus(@PathVariable Integer id, @RequestBody(required = false) DotGiamGiaRequest request) {
        if (request != null && request.getTrangThai() != null) {
            return dotGiamGiaService.setStatus(id, request.getTrangThai());
        }
        return dotGiamGiaService.toggleStatus(id);
    }

    @DeleteMapping("/{id}")
    // Xử lý thao tác đóng, xóa hoặc hủy cho delete.
    public void delete(@PathVariable Integer id) {
        dotGiamGiaService.delete(id);
    }

    @GetMapping("/{id}/san-pham-chi-tiet-ids")
    // Tải hoặc truy xuất dữ liệu cho get selected spct ids.
    public List<Integer> getSelectedSpctIds(@PathVariable Integer id) {
        return dotGiamGiaService.getSelectedSpctIds(id);
    }

    @GetMapping("/san-pham-chi-tiet")
    // Tải hoặc truy xuất dữ liệu cho get san pham chi tiet.
    public List<SanPhamChiTietPromotionView> getSanPhamChiTiet(
            @RequestParam(required = false) LocalDateTime ngayBatDau,
            @RequestParam(required = false) LocalDateTime ngayKetThuc,
            @RequestParam(required = false) Integer excludedPromotionId
    ) {
        return dotGiamGiaService.getSanPhamChiTietKichHoat(
                ngayBatDau, ngayKetThuc, excludedPromotionId);
    }

    // Thực hiện xử lý nghiệp vụ của hàm to request.
    private DotGiamGiaRequest toRequest(DotGiamGia dot) {
        DotGiamGiaRequest request = new DotGiamGiaRequest();
        request.setId(dot.getId());
        request.setMaDotGiamGia(dot.getMaDotGiamGia());
        request.setTenDotGiamGia(dot.getTenDotGiamGia());
        request.setLoaiGiamGia(dot.getLoaiGiamGia());
        request.setGiaTriGiamGia(dot.getGiaTriGiamGia());
        request.setSoTienToiDa(dot.getSoTienToiDa());
        request.setNgayBatDau(dot.getNgayBatDau());
        request.setNgayKetThuc(dot.getNgayKetThuc());
        request.setTrangThai(dot.getTrangThai());
        return request;
    }
}
