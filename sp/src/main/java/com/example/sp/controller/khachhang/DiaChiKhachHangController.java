package com.example.sp.controller.khachhang;

import com.example.sp.dto.khachhang.DiaChiKhachHangDTO;
import com.example.sp.dto.khachhang.DiaChiKhachHangRequest;
import com.example.sp.service.khachhang.DiaChiKhachHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khach-hang/{customerId}/dia-chi")
@RequiredArgsConstructor
public class DiaChiKhachHangController {

    private final DiaChiKhachHangService diaChiKhachHangService;

    @GetMapping
    public List<DiaChiKhachHangDTO> findByCustomer(
            @PathVariable Integer customerId
    ) {
        return diaChiKhachHangService.findByCustomer(customerId);
    }

    @PostMapping
    public ResponseEntity<DiaChiKhachHangDTO> create(
            @PathVariable Integer customerId,
            @Valid @RequestBody DiaChiKhachHangRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diaChiKhachHangService.create(customerId, request));
    }

    @PutMapping("/{addressId}")
    public DiaChiKhachHangDTO update(
            @PathVariable Integer customerId,
            @PathVariable Integer addressId,
            @Valid @RequestBody DiaChiKhachHangRequest request
    ) {
        return diaChiKhachHangService.update(
                customerId,
                addressId,
                request
        );
    }

    @PatchMapping("/{addressId}/mac-dinh")
    public DiaChiKhachHangDTO setDefault(
            @PathVariable Integer customerId,
            @PathVariable Integer addressId
    ) {
        return diaChiKhachHangService.setDefault(
                customerId,
                addressId
        );
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer customerId,
            @PathVariable Integer addressId
    ) {
        diaChiKhachHangService.delete(customerId, addressId);

        return ResponseEntity.noContent().build();
    }
}
