package com.example.sp.controller;

import com.example.sp.dto.promotion.PhieuGiamGiaRequest;
import com.example.sp.model.promotion.PhieuGiamGia;
import com.example.sp.service.promotion.PhieuGiamGiaService;
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
    public PhieuGiamGia getById(@PathVariable Integer id) {
        return phieuGiamGiaService.findById(id);
    }

    @PostMapping
    public PhieuGiamGia create(@RequestBody PhieuGiamGiaRequest request) {
        request.setId(null);
        request.setKhachHangIds(null);
        return phieuGiamGiaService.save(request);
    }

    @PutMapping("/{id}")
    public PhieuGiamGia update(@PathVariable Integer id, @RequestBody PhieuGiamGiaRequest request) {
        request.setId(id);
        request.setKhachHangIds(null);
        return phieuGiamGiaService.save(request);
    }

    @PatchMapping("/{id}/trang-thai")
    public PhieuGiamGia toggleStatus(@PathVariable Integer id) {
        return phieuGiamGiaService.toggleStatus(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        phieuGiamGiaService.delete(id);
    }

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
