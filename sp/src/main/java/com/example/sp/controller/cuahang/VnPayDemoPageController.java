package com.example.sp.controller.cuahang;

import com.example.sp.config.PaymentGatewayProperties;
import com.example.sp.service.cuahang.ShopPaymentGatewayService;
import com.example.sp.service.cuahang.ShopPaymentGatewayService.GatewayReturnResult;
import com.example.sp.service.cuahang.ShopPaymentGatewayService.LocalCheckoutStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class VnPayDemoPageController {

    private final ShopPaymentGatewayService paymentGatewayService;
    private final PaymentGatewayProperties paymentProperties;

    @GetMapping(
            value = {
                    "/vnpay-demo",
                    "/vnpay-demo.html",
                    "/api/shop/payments/demo",
                    "/api/shop/payments/vnpay/checkout"
            },
            produces = MediaType.TEXT_HTML_VALUE
    )
    // Thực hiện xử lý nghiệp vụ của hàm payment page.
    public ResponseEntity<byte[]> paymentPage() throws IOException {
        return htmlPage("templates/VnPayMethodSelection.html");
    }

    @GetMapping(
            value = "/api/shop/payments/vnpay/checkout/card",
            produces = MediaType.TEXT_HTML_VALUE
    )
    // Thực hiện xử lý nghiệp vụ của hàm card payment page.
    public ResponseEntity<byte[]> cardPaymentPage() throws IOException {
        return htmlPage("templates/VnPayCheckout.html");
    }

    @GetMapping("/api/shop/payments/vnpay/checkout/status")
    // Kiểm tra điều kiện và tính hợp lệ cho checkout status.
    public ResponseEntity<LocalCheckoutStatus> checkoutStatus(
            @RequestParam Map<String, String> params
    ) {
        LocalCheckoutStatus status = paymentGatewayService.localVnPayCheckoutStatus(params);
        return status.valid()
                ? ResponseEntity.ok(status)
                : ResponseEntity.badRequest().body(status);
    }

    @GetMapping("/api/shop/payments/vnpay/checkout/scan-confirm")
    // Thực hiện xử lý nghiệp vụ của hàm confirm qr scan.
    public ResponseEntity<Void> confirmQrScan(@RequestParam Map<String, String> params) {
        GatewayReturnResult result = paymentGatewayService.confirmLocalVnPayQrScan(params);
        return paymentResultRedirect(result);
    }

    @GetMapping(
            value = "/api/shop/payments/vnpay/checkout/scan-result",
            produces = MediaType.TEXT_HTML_VALUE
    )
    // Thực hiện xử lý nghiệp vụ của hàm scan result page.
    public ResponseEntity<byte[]> scanResultPage() throws IOException {
        return htmlPage("templates/VnPayScanResult.html");
    }

    @GetMapping(
            value = "/api/shop/payments/qr-demo/checkout",
            produces = MediaType.TEXT_HTML_VALUE
    )
    // Thực hiện xử lý nghiệp vụ của hàm qr demo checkout page.
    public ResponseEntity<byte[]> qrDemoCheckoutPage() throws IOException {
        if (!paymentProperties.getQrDemo().isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        return htmlPage("templates/QrDemoCheckout.html");
    }

    @GetMapping("/api/shop/payments/qr-demo/status")
    // Thực hiện xử lý nghiệp vụ của hàm qr demo checkout status.
    public ResponseEntity<LocalCheckoutStatus> qrDemoCheckoutStatus(
            @RequestParam Map<String, String> params
    ) {
        LocalCheckoutStatus status = paymentGatewayService.qrDemoCheckoutStatus(params);
        return status.valid()
                ? ResponseEntity.ok(status)
                : ResponseEntity.badRequest().body(status);
    }

    @GetMapping("/api/shop/payments/qr-demo/scan-confirm")
    // Thực hiện xử lý nghiệp vụ của hàm confirm qr demo scan.
    public ResponseEntity<Void> confirmQrDemoScan(@RequestParam Map<String, String> params) {
        GatewayReturnResult result = paymentGatewayService.confirmQrDemoScan(params);
        String status = !result.valid() ? "invalid"
                : result.success() ? "success"
                : "failed";
        String target = (paymentGatewayService.isPosOrder(result.orderCode()) ? "/ban-hang-tai-quay" : "/shop")
                + "?paymentResult=" + status
                + "&gateway=" + encode(result.gateway())
                + "&orderCode=" + encode(result.orderCode());
        return ResponseEntity.status(302).location(URI.create(target)).build();
    }

    @GetMapping(
            value = "/api/shop/payments/qr-demo/scan-result",
            produces = MediaType.TEXT_HTML_VALUE
    )
    // Thực hiện xử lý nghiệp vụ của hàm qr demo scan result page.
    public ResponseEntity<byte[]> qrDemoScanResultPage() throws IOException {
        return htmlPage("templates/VnPayScanResult.html");
    }

    // Thực hiện xử lý nghiệp vụ của hàm html page.
    private ResponseEntity<byte[]> htmlPage(String resourcePath) throws IOException {
        byte[] html;
        try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
            html = input.readAllBytes();
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    @PostMapping(
            value = "/api/shop/payments/vnpay/checkout/complete",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    // Thực hiện xử lý nghiệp vụ của hàm complete payment.
    public ResponseEntity<Void> completePayment(@RequestParam Map<String, String> form) {
        Map<String, String> signedParams = new LinkedHashMap<>(form);
        String action = signedParams.remove("outcome");
        String cardNumber = signedParams.remove("testCardNumber");
        String cardDate = signedParams.remove("testCardDate");
        String cvv = signedParams.remove("testCvv");
        String otp = signedParams.remove("testOtp");
        GatewayReturnResult result = paymentGatewayService.completeLocalVnPayTestCheckout(
                signedParams,
                action,
                cardNumber,
                cardDate,
                cvv,
                otp
        );
        return paymentResultRedirect(result);
    }

    @PostMapping(
            value = "/api/shop/payments/vnpay/checkout/wallet/complete",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    // Thực hiện xử lý nghiệp vụ của hàm complete wallet payment.
    public ResponseEntity<Void> completeWalletPayment(@RequestParam Map<String, String> form) {
        Map<String, String> signedParams = new LinkedHashMap<>(form);
        String action = signedParams.remove("outcome");
        String paymentChannel = signedParams.remove("paymentChannel");
        GatewayReturnResult result = paymentGatewayService.completeLocalVnPayWalletCheckout(
                signedParams,
                action,
                paymentChannel
        );
        return paymentResultRedirect(result);
    }

    // Thực hiện xử lý nghiệp vụ của hàm payment result redirect.
    private ResponseEntity<Void> paymentResultRedirect(GatewayReturnResult result) {
        String status = !result.valid() ? "invalid"
                : result.success() ? "success"
                : "failed";
        String target = (paymentGatewayService.isPosOrder(result.orderCode()) ? "/ban-hang-tai-quay" : "/shop")
                + "?paymentResult=" + status
                + "&gateway=VNPAY"
                + "&orderCode=" + encode(result.orderCode());
        return ResponseEntity.status(302).location(URI.create(target)).build();
    }

    // Thực hiện xử lý nghiệp vụ của hàm encode.
    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

}
