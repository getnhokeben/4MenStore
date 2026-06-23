package com.example.sp.controller;

import com.example.sp.dto.promotion.DotGiamGiaRequest;
import com.example.sp.dto.promotion.SanPhamChiTietPromotionView;
import com.example.sp.model.promotion.DotGiamGia;
import com.example.sp.service.promotion.DotGiamGiaService;
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
    public DotGiamGia getById(@PathVariable Integer id) {
        return dotGiamGiaService.findById(id);
    }

    @PostMapping
    public DotGiamGia create(@RequestBody DotGiamGiaRequest request) {
        request.setId(null);
        return dotGiamGiaService.save(request);
    }

    @PutMapping("/{id}")
    public DotGiamGia update(@PathVariable Integer id, @RequestBody DotGiamGiaRequest request) {
        request.setId(id);
        return dotGiamGiaService.save(request);
    }

    @PatchMapping("/{id}/trang-thai")
    public DotGiamGia toggleStatus(@PathVariable Integer id) {
        return dotGiamGiaService.toggleStatus(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        dotGiamGiaService.delete(id);
    }

    @GetMapping("/{id}/san-pham-chi-tiet-ids")
    public List<Integer> getSelectedSpctIds(@PathVariable Integer id) {
        return dotGiamGiaService.getSelectedSpctIds(id);
    }

    @GetMapping("/san-pham-chi-tiet")
    public List<SanPhamChiTietPromotionView> getSanPhamChiTiet() {
        return dotGiamGiaService.getSanPhamChiTietKichHoat();
    }

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
