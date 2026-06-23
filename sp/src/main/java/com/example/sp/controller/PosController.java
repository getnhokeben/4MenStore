package com.example.sp.controller;

import com.example.sp.dto.pos.PosCheckoutRequest;
import com.example.sp.dto.pos.PosCustomerRequest;
import com.example.sp.dto.pos.PosOrderDTO;
import com.example.sp.dto.pos.PosOrderItemDTO;
import com.example.sp.dto.pos.PosOrderItemRequest;
import com.example.sp.dto.pos.PosVoucherRequest;
import com.example.sp.service.pos.PosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;

    @GetMapping(value = {"/ban-hang-tai-quay", "/ban-hang-tai-quay.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> page() throws IOException {
        byte[] html = new ClassPathResource("templates/ban-hang-tai-quay.html").getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    @PostMapping("/api/pos/orders")
    public PosOrderDTO createOrder() {
        return posService.createOrder();
    }

    @GetMapping("/api/pos/orders/{id}")
    public PosOrderDTO getOrder(@PathVariable Integer id) {
        return posService.getOrder(id);
    }

    @PostMapping("/api/pos/orders/{id}/items")
    public PosOrderDTO addItem(@PathVariable Integer id, @Valid @RequestBody PosOrderItemRequest request) {
        return posService.addItem(id, request);
    }

    @PatchMapping("/api/pos/orders/{id}/items/{idHdct}")
    public PosOrderDTO updateItem(@PathVariable Integer id,
                                  @PathVariable Integer idHdct,
                                  @RequestBody Map<String, Integer> body) {
        return posService.updateItem(id, idHdct, body.get("soLuong"));
    }

    @DeleteMapping("/api/pos/orders/{id}/items/{idHdct}")
    public PosOrderDTO removeItem(@PathVariable Integer id, @PathVariable Integer idHdct) {
        return posService.removeItem(id, idHdct);
    }

    @PutMapping("/api/pos/orders/{id}/customer")
    public PosOrderDTO setCustomer(@PathVariable Integer id, @RequestBody PosCustomerRequest request) {
        return posService.setCustomer(id, request);
    }

    @PostMapping("/api/pos/orders/{id}/voucher")
    public PosOrderDTO applyVoucher(@PathVariable Integer id, @Valid @RequestBody PosVoucherRequest request) {
        return posService.applyVoucher(id, request.getMaVoucher());
    }

    @DeleteMapping("/api/pos/orders/{id}/voucher")
    public PosOrderDTO removeVoucher(@PathVariable Integer id) {
        return posService.removeVoucher(id);
    }

    @PostMapping("/api/pos/orders/{id}/checkout")
    public PosOrderDTO checkout(@PathVariable Integer id, @Valid @RequestBody PosCheckoutRequest request) {
        return posService.checkout(id, request);
    }

    @PostMapping("/api/pos/orders/{id}/cancel")
    public PosOrderDTO cancel(@PathVariable Integer id) {
        return posService.cancel(id);
    }

    @GetMapping("/api/pos/variants/by-code")
    public PosOrderItemDTO findVariantByCode(@RequestParam String code) {
        return posService.findVariantByCode(code);
    }
}
