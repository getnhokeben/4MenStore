package com.example.sp.controller.cuahang;

import com.example.sp.dto.cuahang.ShopPaymentRequest;
import com.example.sp.dto.cuahang.ShopPaymentResponse;
import com.example.sp.service.cuahang.ShopPaymentGatewayService;
import com.example.sp.service.cuahang.ShopPaymentGatewayService.GatewayReturnResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shop/payments")
public class ShopPaymentController {

    private final ShopPaymentGatewayService paymentGatewayService;

    @GetMapping("/config")
    public Map<String, Boolean> configuration() {
        return paymentGatewayService.availability();
    }

    @PostMapping("/orders/{orderId}")
    public ShopPaymentResponse createPayment(
            @PathVariable Integer orderId,
            @Valid @RequestBody ShopPaymentRequest request,
            HttpServletRequest servletRequest
    ) {
        return paymentGatewayService.createPayment(
                orderId,
                request.getMaHoaDon(),
                request.getGateway(),
                clientIp(servletRequest)
        );
    }

    @GetMapping("/vnpay/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        return paymentGatewayService.handleVnPayIpn(params);
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        return redirect(paymentGatewayService.evaluateVnPayReturn(params));
    }

    @PostMapping("/zalopay/callback")
    public Map<String, Object> zaloPayCallback(@RequestBody Map<String, Object> callback) {
        return paymentGatewayService.handleZaloPayCallback(callback);
    }

    @GetMapping("/zalopay/return")
    public ResponseEntity<Void> zaloPayReturn(@RequestParam Map<String, String> params) {
        return redirect(paymentGatewayService.evaluateZaloPayReturn(params));
    }

    private ResponseEntity<Void> redirect(GatewayReturnResult result) {
        String status = !result.valid() ? "invalid"
                : result.success() ? "success"
                : result.pending() ? "pending"
                : "failed";
        String target = "/shop?paymentResult=" + status
                + "&gateway=" + encode(result.gateway())
                + "&orderCode=" + encode(result.orderCode());
        return ResponseEntity.status(302).location(URI.create(target)).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
