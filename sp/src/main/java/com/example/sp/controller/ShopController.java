package com.example.sp.controller;

import com.example.sp.dto.shop.ShopOrderRequest;
import com.example.sp.dto.shop.ShopOrderResponse;
import com.example.sp.dto.shop.ShopProductDTO;
import com.example.sp.dto.shop.ShopVariantDTO;
import com.example.sp.dto.shop.ShopVoucherDTO;
import com.example.sp.service.shop.ShopService;
import com.example.sp.service.shop.ShopSessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping(value = {"/", "/shop", "/shop.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> shopPage() throws IOException {
        byte[] html = new ClassPathResource("templates/Shop.html").getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    @GetMapping("/api/shop/products")
    public Page<ShopProductDTO> products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loaiAo,
            @RequestParam(required = false) String kichCo,
            @RequestParam(required = false) String mauSac,
            @RequestParam(required = false) BigDecimal giaMin,
            @RequestParam(required = false) BigDecimal giaMax,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return shopService.getProducts(keyword, loaiAo, kichCo, mauSac, giaMin, giaMax, sort, page, size);
    }

    @GetMapping("/api/shop/products/{id}")
    public ShopProductDTO product(@PathVariable Integer id) {
        return shopService.getProduct(id);
    }

    @GetMapping("/api/shop/products/{id}/variants")
    public List<ShopVariantDTO> variants(@PathVariable Integer id) {
        return shopService.getVariants(id);
    }

    @GetMapping("/api/shop/vouchers/{code}")
    public ShopVoucherDTO voucher(
            @PathVariable String code,
            @RequestParam(defaultValue = "0") BigDecimal subtotal
    ) {
        return shopService.getVoucher(code, subtotal);
    }

    @PostMapping("/api/shop/orders")
    public ShopOrderResponse createOrder(@Valid @RequestBody ShopOrderRequest request, HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(ShopSessionKeys.CUSTOMER_ID);
        return shopService.createOrder(request, customerId);
    }
}
