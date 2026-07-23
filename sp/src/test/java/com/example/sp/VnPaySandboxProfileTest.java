package com.example.sp;

import com.example.sp.config.PaymentGatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "VNPAY_HASH_SECRET=test-only-secret")
@ActiveProfiles("vnpay-sandbox")
class VnPaySandboxProfileTest {

    @Autowired
    private PaymentGatewayProperties properties;

    @Test
    void sandboxProfileEnablesConfiguredVnPayGateway() {
        assertTrue(properties.isVnPayConfigured());
        assertTrue(properties.getVnpay().isLocalCheckoutEnabled());
        assertEquals("L5HQXLHO", properties.getVnpay().getTmnCode());
        assertEquals("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                properties.getVnpay().getPaymentUrl());
    }
}
