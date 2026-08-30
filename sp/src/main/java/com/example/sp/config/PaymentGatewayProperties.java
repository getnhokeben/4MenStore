package com.example.sp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentGatewayProperties {

    private String publicBaseUrl = "http://localhost:8081";
    private final VnPay vnpay = new VnPay();
    private final QrDemo qrDemo = new QrDemo();
    private final Momo momo = new Momo();
    private final ZaloPay zalopay = new ZaloPay();

    // Kiểm tra điều kiện và tính hợp lệ cho is vn pay configured.
    public boolean isVnPayConfigured() {
        return vnpay.enabled && hasText(vnpay.tmnCode) && hasText(vnpay.hashSecret)
                && hasText(vnpay.paymentUrl);
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is momo configured.
    public boolean isMomoConfigured() {
        return momo.enabled && hasText(momo.partnerCode) && hasText(momo.accessKey)
                && hasText(momo.secretKey) && hasText(momo.endpoint);
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is zalo pay configured.
    public boolean isZaloPayConfigured() {
        return zalopay.enabled && zalopay.appId > 0 && hasText(zalopay.key1)
                && hasText(zalopay.key2) && hasText(zalopay.endpoint)
                && hasText(zalopay.queryEndpoint);
    }

    // Thực hiện xử lý nghiệp vụ của hàm base url.
    public String baseUrl() {
        String value = hasText(publicBaseUrl) ? publicBaseUrl.trim() : "http://localhost:8081";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    // Kiểm tra điều kiện và tính hợp lệ cho has text.
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Getter
    @Setter
    public static class VnPay {
        private boolean enabled;
        private boolean reconcileOnReturn;
        private boolean localCheckoutEnabled;
        private String tmnCode = "";
        private String hashSecret = "";
        private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    }

    @Getter
    @Setter
    public static class QrDemo {
        /**
         * Enables the local, non-monetary QR confirmation flows used by the demo shop.
         */
        private boolean enabled = true;
        /**
         * Optional HMAC secret. A process-local fallback is used when this is blank.
         */
        private String secret = "";
    }

    @Getter
    @Setter
    public static class Momo {
        private boolean enabled;
        private String partnerCode = "";
        private String accessKey = "";
        private String secretKey = "";
        private String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
    }

    @Getter
    @Setter
    public static class ZaloPay {
        private boolean enabled;
        private int appId;
        private String key1 = "";
        private String key2 = "";
        private String endpoint = "https://sb-openapi.zalopay.vn/v2/create";
        private String queryEndpoint = "https://sb-openapi.zalopay.vn/v2/query";
    }
}
