package com.example.sp.controller.cuahang;

import com.example.sp.dto.cuahang.ShopOrderRequest;
import com.example.sp.dto.cuahang.ShopOrderResponse;
import com.example.sp.dto.cuahang.ShopOrderHistoryDTO;
import com.example.sp.dto.cuahang.ShopProductDTO;
import com.example.sp.dto.cuahang.ShopVariantDTO;
import com.example.sp.dto.cuahang.ShopVoucherDTO;
import com.example.sp.dto.banhang.PosShippingRequest;
import com.example.sp.service.giaohang.GhnShippingService;
import com.example.sp.service.cuahang.ShopService;
import com.example.sp.service.cuahang.ShopSessionKeys;
import com.example.sp.service.tienich.MoneyRoundingUtil;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final GhnShippingService ghnShippingService;

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
            @RequestParam(required = false) String chatLieu,
            @RequestParam(required = false) String xuatXu,
            @RequestParam(required = false) String phongCachMac,
            @RequestParam(required = false) String kieuDang,
            @RequestParam(required = false) BigDecimal giaMin,
            @RequestParam(required = false) BigDecimal giaMax,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return shopService.getProducts(
                keyword,
                loaiAo,
                kichCo,
                mauSac,
                chatLieu,
                xuatXu,
                phongCachMac,
                kieuDang,
                giaMin,
                giaMax,
                sort,
                page,
                size
        );
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

    @GetMapping("/api/shop/vouchers")
    public List<ShopVoucherDTO> vouchers(@RequestParam(defaultValue = "0") BigDecimal subtotal) {
        return shopService.getVouchers(subtotal);
    }

    @PostMapping("/api/shop/orders")
    public ShopOrderResponse createOrder(@Valid @RequestBody ShopOrderRequest request, HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(ShopSessionKeys.CUSTOMER_ID);
        return shopService.createOrder(request, customerId);
    }

    @GetMapping("/api/shop/orders/history")
    public ResponseEntity<List<ShopOrderHistoryDTO>> orderHistory(HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(ShopSessionKeys.CUSTOMER_ID);
        if (customerId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(shopService.getOrderHistory(customerId));
    }

    @GetMapping("/api/shop/orders/lookup")
    public ShopOrderHistoryDTO lookupOrder(@RequestParam String maHoaDon) {
        return shopService.lookupOrder(maHoaDon);
    }

    @GetMapping("/api/shop/shipping/provinces")
    public ResponseEntity<String> provinces() {
        return json(ghnShippingService.provinces());
    }

    @GetMapping("/api/shop/shipping/districts")
    public ResponseEntity<String> districts(@RequestParam Integer provinceId) {
        return json(ghnShippingService.districts(provinceId));
    }

    @GetMapping("/api/shop/shipping/wards")
    public ResponseEntity<String> wards(@RequestParam Integer districtId) {
        return json(ghnShippingService.wards(districtId));
    }

    @PostMapping("/api/shop/shipping/fee")
    public Map<String, BigDecimal> shippingFee(@RequestBody PosShippingRequest request) {
        BigDecimal fee = request.getDistrictId() != null && request.getDistrictId() > 0
                ? ghnShippingService.calculateFee(
                        request.getDistrictId(),
                        request.getWardCode(),
                        request.getPhiVanChuyen()
                )
                : ghnShippingService.calculateTwoTierFee(
                        request.getProvinceName(),
                        request.getWardName(),
                        request.getPhiVanChuyen()
                );
        return Map.of("fee", MoneyRoundingUtil.roundNonNegative(fee));
    }

    private ResponseEntity<String> json(String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
