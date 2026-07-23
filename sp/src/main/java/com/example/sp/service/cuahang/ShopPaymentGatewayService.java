package com.example.sp.service.cuahang;

import com.example.sp.config.PaymentGatewayProperties;
import com.example.sp.dto.cuahang.ShopPaymentResponse;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.HoaDonChiTiet;
import com.example.sp.model.hoadon.LichSuThanhToan;
import com.example.sp.model.hoadon.PhuongThucThanhToan;
import com.example.sp.model.hoadon.ThanhToan;
import com.example.sp.repository.hoadon.HoaDonChiTietRepository;
import com.example.sp.repository.hoadon.HoaDonRepository;
import com.example.sp.repository.hoadon.LichSuThanhToanRepository;
import com.example.sp.repository.hoadon.PhuongThucThanhToanRepository;
import com.example.sp.repository.hoadon.ThanhToanRepository;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.service.tonkho.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper; // sửa import

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopPaymentGatewayService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter ZALOPAY_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Map<String, VnPayTestCard> VNPAY_SUCCESS_TEST_CARDS = Map.ofEntries(
            Map.entry("9704198526191432198", new VnPayTestCard("07/15", "", "123456")),
            Map.entry("4456530000001005", new VnPayTestCard("12/26", "123", "")),
            Map.entry("4456530000001096", new VnPayTestCard("12/26", "123", "")),
            Map.entry("5200000000001005", new VnPayTestCard("12/26", "123", "")),
            Map.entry("5200000000001096", new VnPayTestCard("12/26", "123", "")),
            Map.entry("3337000000000008", new VnPayTestCard("12/26", "123", "")),
            Map.entry("3337000000200004", new VnPayTestCard("12/24", "123", "")),
            Map.entry("9704000000000018", new VnPayTestCard("03/07", "", "otp")),
            Map.entry("9704020000000016", new VnPayTestCard("03/07", "", "otp")),
            Map.entry("9704310005819191", new VnPayTestCard("10/26", "", ""))
    );

    private final PaymentGatewayProperties properties;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final PhuongThucThanhToanRepository paymentMethodRepository;
    private final LichSuThanhToanRepository paymentHistoryRepository;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile HttpClient httpClient;
    private final String qrDemoFallbackSecret = UUID.randomUUID().toString();

    public Map<String, Boolean> availability() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("COD", true);
        result.put("BANKING", properties.getQrDemo().isEnabled());
        result.put("VNPAY", properties.getQrDemo().isEnabled() || properties.isVnPayConfigured());
        result.put("ZALOPAY", properties.getQrDemo().isEnabled() || properties.isZaloPayConfigured());
        return result;
    }

    @Transactional
    public ShopPaymentResponse createPayment(
            Integer orderId,
            String orderCode,
            String gatewayValue,
            String clientIp
    ) {
        HoaDon order = requirePayableOrder(orderId, orderCode);
        String gateway = normalizeGateway(gatewayValue);
        ensureGatewayConfigured(gateway);

        return switch (gateway) {
            case "BANKING" -> createQrDemoPayment(order, gateway);
            case "VNPAY" -> createVnPayPayment(order, clientIp);
            case "ZALOPAY" -> createZaloPayPayment(order);
            default -> throw new IllegalArgumentException("Cổng thanh toán không được hỗ trợ");
        };
    }

    private ShopPaymentResponse createVnPayPayment(HoaDon order, String clientIp) {
        if (!properties.isVnPayConfigured() && properties.getQrDemo().isEnabled()) {
            return createQrDemoPayment(order, "VNPAY");
        }

        PaymentGatewayProperties.VnPay config = properties.getVnpay();
        String transactionRef = transactionRef("VP", order.getId());
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);

        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", String.valueOf(orderAmount(order) * 100L));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getMaHoaDon());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", callbackUrl("/api/shop/payments/vnpay/return"));
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", now.format(VNPAY_TIME));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNPAY_TIME));

        String query = queryString(params);
        String signature = hmac("HmacSHA512", config.getHashSecret(), query);
        String paymentEndpoint = config.isLocalCheckoutEnabled()
                ? callbackUrl("/api/shop/payments/vnpay/checkout")
                : config.getPaymentUrl();
        String paymentUrl = paymentEndpoint + "?" + query + "&vnp_SecureHash=" + signature;
        createPendingPayment(order, "VNPAY", transactionRef);
        return ShopPaymentResponse.builder()
                .gateway("VNPAY")
                .paymentUrl(paymentUrl)
                .transactionRef(transactionRef)
                .build();
    }

    private ShopPaymentResponse createMomoPayment(HoaDon order) {
        PaymentGatewayProperties.Momo config = properties.getMomo();
        String transactionRef = transactionRef("MM", order.getId());
        String requestId = transactionRef;
        String amount = String.valueOf(orderAmount(order));
        String orderInfo = "Thanh toan don hang " + order.getMaHoaDon();
        String redirectUrl = callbackUrl("/api/shop/payments/momo/return");
        String ipnUrl = callbackUrl("/api/shop/payments/momo/ipn");
        String extraData = "";
        String requestType = "captureWallet";

        String rawSignature = "accessKey=" + config.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + transactionRef
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + config.getPartnerCode()
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", config.getPartnerCode());
        payload.put("partnerName", "4MenStore");
        payload.put("storeId", "4MenStore");
        payload.put("requestId", requestId);
        payload.put("amount", orderAmount(order));
        payload.put("orderId", transactionRef);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", redirectUrl);
        payload.put("ipnUrl", ipnUrl);
        payload.put("lang", "vi");
        payload.put("extraData", extraData);
        payload.put("requestType", requestType);
        payload.put("signature", hmac("HmacSHA256", config.getSecretKey(), rawSignature));

        Map<String, Object> response = postJson(config.getEndpoint(), payload);
        if (number(response.get("resultCode")) != 0L || text(response.get("payUrl")).isBlank()) {
            throw new IllegalStateException("MoMo từ chối tạo giao dịch: " + text(response.get("message")));
        }

        createPendingPayment(order, "MOMO", transactionRef);
        return ShopPaymentResponse.builder()
                .gateway("MOMO")
                .paymentUrl(text(response.get("payUrl")))
                .transactionRef(transactionRef)
                .build();
    }

    private ShopPaymentResponse createZaloPayPayment(HoaDon order) {
        if (properties.getQrDemo().isEnabled()) {
            return createQrDemoPayment(order, "ZALOPAY");
        }

        PaymentGatewayProperties.ZaloPay config = properties.getZalopay();
        long appTime = System.currentTimeMillis();
        String transactionRef = LocalDateTime.now(VIETNAM_ZONE).format(ZALOPAY_DATE)
                + "_ZP" + order.getId() + "T" + appTime;
        String appUser = "4MenStore";
        String amount = String.valueOf(orderAmount(order));
        String item = "[]";
        String redirectUrl = callbackUrl("/api/shop/payments/zalopay/return")
                + "?orderCode=" + urlEncode(order.getMaHoaDon());

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("redirecturl", redirectUrl);
        embed.put("preferred_payment_method", new String[]{"zalopay_wallet"});
        String embedData = writeJson(embed);
        String macInput = config.getAppId() + "|" + transactionRef + "|" + appUser + "|"
                + amount + "|" + appTime + "|" + embedData + "|" + item;

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("app_id", String.valueOf(config.getAppId()));
        payload.put("app_user", appUser);
        payload.put("app_trans_id", transactionRef);
        payload.put("app_time", String.valueOf(appTime));
        payload.put("expire_duration_seconds", "900");
        payload.put("amount", amount);
        payload.put("description", "Thanh toan don hang " + order.getMaHoaDon());
        payload.put("callback_url", callbackUrl("/api/shop/payments/zalopay/callback"));
        payload.put("item", item);
        payload.put("embed_data", embedData);
        payload.put("bank_code", "");
        payload.put("mac", hmac("HmacSHA256", config.getKey1(), macInput));

        Map<String, Object> response = postForm(config.getEndpoint(), payload);
        if (number(response.get("return_code")) != 1L || text(response.get("order_url")).isBlank()) {
            throw new IllegalStateException("ZaloPay từ chối tạo giao dịch: " + text(response.get("return_message")));
        }

        createPendingPayment(order, "ZALOPAY", transactionRef);
        return ShopPaymentResponse.builder()
                .gateway("ZALOPAY")
                .paymentUrl(text(response.get("order_url")))
                .transactionRef(transactionRef)
                .build();
    }

    private ShopPaymentResponse createQrDemoPayment(HoaDon order, String gateway) {
        String transactionRef = transactionRef("QR", order.getId());
        long expiresAt = System.currentTimeMillis() / 1_000L + 15 * 60;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("gateway", gateway);
        params.put("ref", transactionRef);
        params.put("amount", String.valueOf(orderAmount(order)));
        params.put("expires", String.valueOf(expiresAt));
        params.put("token", qrDemoToken(gateway, transactionRef, orderAmount(order), expiresAt));

        createPendingPayment(order, gateway, transactionRef);
        return ShopPaymentResponse.builder()
                .gateway(gateway)
                .paymentUrl(callbackUrl("/api/shop/payments/qr-demo/checkout") + "?" + queryString(params))
                .transactionRef(transactionRef)
                .build();
    }

    @Transactional
    public Map<String, String> handleVnPayIpn(Map<String, String> params) {
        if (!verifyVnPaySignature(params)) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }
        String ref = params.get("vnp_TxnRef");
        ThanhToan payment = findPayment(ref);
        if (payment == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }
        long receivedAmount = parseLong(params.get("vnp_Amount")) / 100L;
        if (!amountMatches(payment.getHoaDon(), receivedAmount)) {
            return Map.of("RspCode", "04", "Message", "Invalid amount");
        }
        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));
        if (success) {
            markPaid(payment, "VNPAY", text(params.get("vnp_TransactionNo")));
        } else {
            markFailed(payment);
        }
        return Map.of("RspCode", "00", "Message", "Confirm success");
    }

    @Transactional
    public GatewayReturnResult evaluateVnPayReturn(Map<String, String> params) {
        boolean signatureValid = verifyVnPaySignature(params);
        ThanhToan payment = signatureValid ? findPayment(params.get("vnp_TxnRef")) : null;
        boolean valid = signatureValid && payment != null
                && amountMatches(payment.getHoaDon(), parseLong(params.get("vnp_Amount")) / 100L);
        boolean approved = valid
                && "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));
        if (approved && properties.getVnpay().isReconcileOnReturn()) {
            markPaid(payment, "VNPAY", text(params.get("vnp_TransactionNo")));
        }
        return returnResult(valid, approved, payment, "VNPAY");
    }

    @Transactional
    public GatewayReturnResult completeLocalVnPayCheckout(Map<String, String> params, boolean approved) {
        return completeLocalVnPayCheckout(params, approved, "LOCAL-");
    }

    private GatewayReturnResult completeLocalVnPayCheckout(
            Map<String, String> params,
            boolean approved,
            String transactionPrefix
    ) {
        if (!properties.getVnpay().isLocalCheckoutEnabled()) {
            return new GatewayReturnResult(false, false, false, "", "VNPAY");
        }

        boolean signatureValid = verifyVnPaySignature(params);
        ThanhToan payment = signatureValid ? findPayment(params.get("vnp_TxnRef")) : null;
        boolean valid = signatureValid && payment != null
                && amountMatches(payment.getHoaDon(), parseLong(params.get("vnp_Amount")) / 100L);
        if (!valid) {
            return returnResult(false, false, payment, "VNPAY");
        }

        if (payment.getHoaDon().getNgayThanhToan() != null) {
            return returnResult(true, true, payment, "VNPAY");
        }

        if (approved) {
            markPaid(payment, "VNPAY", transactionPrefix + payment.getMaGiaoDich());
        } else {
            markFailed(payment);
        }
        return returnResult(true, approved, payment, "VNPAY");
    }

    @Transactional
    public GatewayReturnResult completeLocalVnPayWalletCheckout(
            Map<String, String> params,
            String action,
            String paymentChannel
    ) {
        String channel = paymentChannel == null
                ? ""
                : paymentChannel.trim().toUpperCase(Locale.ROOT);
        if (!"QR".equals(channel) && !"APP".equals(channel)) {
            return new GatewayReturnResult(false, false, false, "", "VNPAY");
        }

        boolean approved = !"cancel".equalsIgnoreCase(action);
        return completeLocalVnPayCheckout(params, approved, "LOCAL-" + channel + "-");
    }

    @Transactional
    public GatewayReturnResult confirmLocalVnPayQrScan(Map<String, String> params) {
        if (isExpiredVnPayCheckout(params)) {
            return new GatewayReturnResult(false, false, false, "", "VNPAY");
        }
        return completeLocalVnPayCheckout(params, true, "LOCAL-QR-SCAN-");
    }

    @Transactional(readOnly = true)
    public LocalCheckoutStatus localVnPayCheckoutStatus(Map<String, String> params) {
        if (!properties.getVnpay().isLocalCheckoutEnabled()
                || isExpiredVnPayCheckout(params)
                || !verifyVnPaySignature(params)) {
            return new LocalCheckoutStatus(false, false, "");
        }

        ThanhToan payment = findPayment(params.get("vnp_TxnRef"));
        boolean valid = payment != null
                && payment.getHoaDon() != null
                && amountMatches(payment.getHoaDon(), parseLong(params.get("vnp_Amount")) / 100L);
        if (!valid) {
            return new LocalCheckoutStatus(false, false, "");
        }

        boolean paid = isPaymentSuccessful(payment);
        return new LocalCheckoutStatus(true, paid, payment.getHoaDon().getMaHoaDon());
    }

    @Transactional
    public GatewayReturnResult confirmQrDemoScan(Map<String, String> params) {
        QrDemoPayment demoPayment = validateQrDemoPayment(params);
        if (demoPayment == null) {
            return new GatewayReturnResult(false, false, false, "", "");
        }

        ThanhToan payment = demoPayment.payment();
        if (!isPaymentSuccessful(payment)) {
            markPaid(payment, demoPayment.gateway(), "QR-DEMO-SCAN-" + payment.getMaGiaoDich());
        }
        return returnResult(true, true, payment, demoPayment.gateway());
    }

    @Transactional(readOnly = true)
    public LocalCheckoutStatus qrDemoCheckoutStatus(Map<String, String> params) {
        QrDemoPayment demoPayment = validateQrDemoPayment(params);
        if (demoPayment == null) {
            return new LocalCheckoutStatus(false, false, "");
        }

        ThanhToan payment = demoPayment.payment();
        boolean paid = isPaymentSuccessful(payment);
        return new LocalCheckoutStatus(true, paid, payment.getHoaDon().getMaHoaDon());
    }

    @Transactional
    public GatewayReturnResult completeLocalVnPayTestCheckout(
            Map<String, String> params,
            String action,
            String cardNumber,
            String cardDate,
            String cvv,
            String otp
    ) {
        if ("cancel".equalsIgnoreCase(action)) {
            return completeLocalVnPayCheckout(params, false);
        }

        String normalizedCard = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        VnPayTestCard testCard = VNPAY_SUCCESS_TEST_CARDS.get(normalizedCard);
        boolean approved = testCard != null
                && testCard.date().equals(cardDate == null ? "" : cardDate.trim())
                && (testCard.cvv().isBlank() || testCard.cvv().equals(cvv == null ? "" : cvv.trim()))
                && (testCard.otp().isBlank() || testCard.otp().equalsIgnoreCase(otp == null ? "" : otp.trim()));
        return completeLocalVnPayCheckout(params, approved);
    }

    @Transactional
    public boolean handleMomoIpn(Map<String, Object> params) {
        if (!verifyMomoSignature(params)) return false;
        ThanhToan payment = findPayment(text(params.get("orderId")));
        if (payment == null || !amountMatches(payment.getHoaDon(), number(params.get("amount")))) return false;
        if (number(params.get("resultCode")) == 0L) {
            markPaid(payment, "MOMO", text(params.get("transId")));
        } else {
            markFailed(payment);
        }
        return true;
    }

    @Transactional(readOnly = true)
    public GatewayReturnResult evaluateMomoReturn(Map<String, String> query) {
        Map<String, Object> params = new LinkedHashMap<>(query);
        boolean signatureValid = verifyMomoSignature(params);
        ThanhToan payment = signatureValid ? findPayment(text(params.get("orderId"))) : null;
        boolean valid = signatureValid && payment != null
                && amountMatches(payment.getHoaDon(), number(params.get("amount")));
        boolean approved = valid
                && number(params.get("resultCode")) == 0L;
        return returnResult(valid, approved, payment, "MOMO");
    }

    @Transactional
    public Map<String, Object> handleZaloPayCallback(Map<String, Object> callback) {
        if (!properties.isZaloPayConfigured()) {
            return Map.of("return_code", -1, "return_message", "Gateway is not configured");
        }
        if (number(callback.get("type")) != 1L) {
            return Map.of("return_code", 2, "return_message", "Callback type is invalid");
        }
        String data = text(callback.get("data"));
        String expectedMac = hmac("HmacSHA256", properties.getZalopay().getKey2(), data);
        if (!constantTimeEquals(expectedMac, text(callback.get("mac")))) {
            return Map.of("return_code", -1, "return_message", "Invalid signature");
        }

        Map<String, Object> payload;
        try {
            payload = readJson(data);
        } catch (IllegalArgumentException exception) {
            return Map.of("return_code", 2, "return_message", "Callback data is invalid");
        }
        if (number(payload.get("app_id")) != properties.getZalopay().getAppId()) {
            return Map.of("return_code", 2, "return_message", "App ID is invalid");
        }
        ThanhToan payment = findPayment(text(payload.get("app_trans_id")));
        if (payment == null
                || !amountMatches(payment.getHoaDon(), number(payload.get("amount")))
                || number(payload.get("zp_trans_id")) <= 0L) {
            return Map.of("return_code", 2, "return_message", "Order or amount is invalid");
        }
        markPaid(payment, "ZALOPAY", text(payload.get("zp_trans_id")));
        return Map.of("return_code", 1, "return_message", "success");
    }

    @Transactional
    public GatewayReturnResult evaluateZaloPayReturn(Map<String, String> query) {
        String orderCode = query == null ? "" : text(query.get("orderCode"));
        if (!verifyZaloPayRedirect(query)) {
            return new GatewayReturnResult(false, false, false, orderCode, "ZALOPAY");
        }

        String transactionRef = text(query.get("apptransid"));
        ThanhToan payment = findPayment(transactionRef);
        boolean valid = payment != null
                && payment.getHoaDon() != null
                && payment.getHoaDon().getMaHoaDon() != null
                && payment.getHoaDon().getMaHoaDon().equalsIgnoreCase(orderCode)
                && amountMatches(payment.getHoaDon(), parseLong(query.get("amount")));
        if (!valid) {
            return new GatewayReturnResult(false, false, false, orderCode, "ZALOPAY");
        }

        HoaDon order = payment.getHoaDon();
        if (order.getNgayThanhToan() != null) {
            return new GatewayReturnResult(true, true, false, order.getMaHoaDon(), "ZALOPAY");
        }
        if (!"1".equals(query.get("status"))) {
            return new GatewayReturnResult(true, false, false, order.getMaHoaDon(), "ZALOPAY");
        }

        try {
            Map<String, Object> status = queryZaloPayOrder(transactionRef);
            long returnCode = number(status.get("return_code"));
            if (returnCode == 1L
                    && amountMatches(order, number(status.get("amount")))
                    && number(status.get("zp_trans_id")) > 0L) {
                markPaid(payment, "ZALOPAY", text(status.get("zp_trans_id")));
                return new GatewayReturnResult(true, true, false, order.getMaHoaDon(), "ZALOPAY");
            }
            if (returnCode == 2L) {
                return new GatewayReturnResult(true, false, false, order.getMaHoaDon(), "ZALOPAY");
            }
        } catch (IllegalStateException ignored) {
            // The callback can still arrive after the customer returns to the shop.
        }
        return new GatewayReturnResult(true, false, true, order.getMaHoaDon(), "ZALOPAY");
    }

    private boolean verifyZaloPayRedirect(Map<String, String> query) {
        if (!properties.isZaloPayConfigured() || query == null) return false;
        PaymentGatewayProperties.ZaloPay config = properties.getZalopay();
        if (parseLong(query.get("appid")) != config.getAppId()) return false;

        String macInput = text(query.get("appid")) + "|"
                + text(query.get("apptransid")) + "|"
                + text(query.get("pmcid")) + "|"
                + text(query.get("bankcode")) + "|"
                + text(query.get("amount")) + "|"
                + text(query.get("discountamount")) + "|"
                + text(query.get("status"));
        String expected = hmac("HmacSHA256", config.getKey2(), macInput);
        return constantTimeEquals(expected, query.get("checksum"));
    }

    private Map<String, Object> queryZaloPayOrder(String transactionRef) {
        PaymentGatewayProperties.ZaloPay config = properties.getZalopay();
        String macInput = config.getAppId() + "|" + transactionRef + "|" + config.getKey1();
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("app_id", String.valueOf(config.getAppId()));
        payload.put("app_trans_id", transactionRef);
        payload.put("mac", hmac("HmacSHA256", config.getKey1(), macInput));
        return postForm(config.getQueryEndpoint(), payload);
    }

    private boolean verifyVnPaySignature(Map<String, String> source) {
        if (!properties.isVnPayConfigured() || source == null) return false;
        if (!properties.getVnpay().getTmnCode().equals(source.get("vnp_TmnCode"))) return false;
        String received = source.get("vnp_SecureHash");
        TreeMap<String, String> signed = new TreeMap<>();
        source.forEach((key, value) -> {
            if (key != null && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key) && !"vnp_SecureHashType".equals(key)
                    && value != null && !value.isBlank()) {
                signed.put(key, value);
            }
        });
        String expected = hmac("HmacSHA512", properties.getVnpay().getHashSecret(), queryString(signed));
        return constantTimeEquals(expected, received);
    }

    private boolean verifyMomoSignature(Map<String, ?> params) {
        if (!properties.isMomoConfigured() || params == null) return false;
        String raw = "accessKey=" + properties.getMomo().getAccessKey()
                + "&amount=" + text(params.get("amount"))
                + "&extraData=" + text(params.get("extraData"))
                + "&message=" + text(params.get("message"))
                + "&orderId=" + text(params.get("orderId"))
                + "&orderInfo=" + text(params.get("orderInfo"))
                + "&orderType=" + text(params.get("orderType"))
                + "&partnerCode=" + text(params.get("partnerCode"))
                + "&payType=" + text(params.get("payType"))
                + "&requestId=" + text(params.get("requestId"))
                + "&responseTime=" + text(params.get("responseTime"))
                + "&resultCode=" + text(params.get("resultCode"))
                + "&transId=" + text(params.get("transId"));
        String expected = hmac("HmacSHA256", properties.getMomo().getSecretKey(), raw);
        return constantTimeEquals(expected, text(params.get("signature")));
    }

    private GatewayReturnResult returnResult(
            boolean valid,
            boolean approved,
            ThanhToan payment,
            String gateway
    ) {
        String code = payment == null || payment.getHoaDon() == null
                ? ""
                : payment.getHoaDon().getMaHoaDon();
        boolean paid = payment != null && payment.getHoaDon() != null
                && payment.getHoaDon().getNgayThanhToan() != null;
        return new GatewayReturnResult(valid, approved && paid, valid && approved && !paid, code, gateway);
    }

    private HoaDon requirePayableOrder(Integer orderId, String orderCode) {
        HoaDon order = hoaDonRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (order.getMaHoaDon() == null || !order.getMaHoaDon().equalsIgnoreCase(orderCode == null ? "" : orderCode.trim())) {
            throw new IllegalArgumentException("Mã đơn hàng không khớp");
        }
        if (order.getNgayThanhToan() != null) {
            throw new IllegalArgumentException("Đơn hàng đã được thanh toán");
        }
        if (!"Chờ thanh toán online".equalsIgnoreCase(order.getTrangThai())) {
            throw new IllegalArgumentException("Đơn hàng không ở trạng thái chờ thanh toán online");
        }
        if (orderAmount(order) <= 0L) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ");
        }
        return order;
    }

    private void ensureOrderGateway(HoaDon order, String gateway) {
        String note = order.getGhiChu() == null ? "" : order.getGhiChu().toUpperCase(Locale.ROOT);
        if (!note.contains("PHƯƠNG THỨC THANH TOÁN: " + gateway)) {
            throw new IllegalArgumentException("Phương thức thanh toán không khớp với đơn hàng");
        }
    }

    private void ensureGatewayConfigured(String gateway) {
        boolean configured = switch (gateway) {
            case "BANKING" -> properties.getQrDemo().isEnabled();
            case "VNPAY" -> properties.getQrDemo().isEnabled() || properties.isVnPayConfigured();
            case "ZALOPAY" -> properties.getQrDemo().isEnabled() || properties.isZaloPayConfigured();
            default -> false;
        };
        if (!configured) {
            throw new IllegalStateException("Cổng " + gateway + " chưa được cấu hình đầy đủ");
        }
    }

    private void createPendingPayment(HoaDon order, String gateway, String transactionRef) {
        PhuongThucThanhToan method = findOrCreatePaymentMethod(gateway, paymentName(gateway));
        thanhToanRepository.save(ThanhToan.builder()
                .hoaDon(order)
                .phuongThucThanhToan(method)
                .maGiaoDich(transactionRef)
                .soTien(MoneyRoundingUtil.roundNonNegative(order.getTongTienThanhToan()))
                .trangThai("Chờ thanh toán")
                .build());
    }

    private void markPaid(ThanhToan payment, String gateway, String externalTransactionId) {
        if (payment == null || payment.getHoaDon() == null) return;
        if (isPaymentSuccessful(payment)) return;

        LocalDateTime paidAt = LocalDateTime.now();
        HoaDon order = payment.getHoaDon();
        payment.setTrangThai("Thành công");
        payment.setThoiGianThanhToan(paidAt);
        thanhToanRepository.save(payment);

        // A customer may retry and create more than one valid gateway transaction.
        // The first successful callback owns the order; later callbacks stay idempotent.
        if (order.getNgayThanhToan() != null) return;

        confirmPaidOrderStock(order);
        order.setNgayThanhToan(paidAt);
        order.setNgayCapNhat(paidAt);
        // Successful online payment owns and deducts the reserved stock, so
        // the order is confirmed in the same transaction.
        order.setTrangThai("Đã xác nhận");
        hoaDonRepository.save(order);

        String historyCode = gateway + "-" + (externalTransactionId == null || externalTransactionId.isBlank()
                ? payment.getMaGiaoDich()
                : externalTransactionId);
        if (!paymentHistoryRepository.existsByMaGiaoDich(historyCode)) {
            paymentHistoryRepository.save(LichSuThanhToan.builder()
                    .hoaDon(order)
                    .maGiaoDich(historyCode)
                    .soTien(MoneyRoundingUtil.roundNonNegative(order.getTongTienThanhToan()))
                    .ngayThanhToan(paidAt)
                    .hinhThucThanhToan(paymentName(gateway))
                    .loaiThanhToan("Thanh toán hóa đơn")
                    .trangThai("Thành công")
                    .build());
        }
    }

    private void confirmPaidOrderStock(HoaDon order) {
        if (Boolean.TRUE.equals(order.getDaTruTon())) {
            return;
        }

        var items = hoaDonChiTietRepository.findByHoaDon_Id(order.getId());
        if (items.isEmpty()) {
            throw new IllegalStateException("Hóa đơn chưa có sản phẩm để trừ tồn kho");
        }

        boolean hasReservation = Boolean.TRUE.equals(order.getDaGiuTon());
        for (HoaDonChiTiet item : items) {
            if (item.getChiTietSanPham() == null || item.getChiTietSanPham().getIdSpct() == null) {
                throw new IllegalStateException("Chi tiết hóa đơn không có biến thể sản phẩm");
            }
            int quantity = item.getSoLuong() == null ? 0 : item.getSoLuong();
            if (quantity <= 0) continue;
            if (hasReservation) {
                inventoryService.confirmOnlineReservation(item.getChiTietSanPham().getIdSpct(), quantity);
            } else {
                inventoryService.deductOnlineStock(item.getChiTietSanPham().getIdSpct(), quantity);
            }
        }
        order.setDaGiuTon(false);
        order.setDaTruTon(true);
    }

    private void markFailed(ThanhToan payment) {
        if (payment != null && !isPaymentSuccessful(payment)) {
            payment.setTrangThai("Thất bại");
            thanhToanRepository.save(payment);
        }
    }

    private boolean isPaymentSuccessful(ThanhToan payment) {
        return payment != null
                && payment.getHoaDon() != null
                && payment.getHoaDon().getNgayThanhToan() != null;
    }

    private ThanhToan findPayment(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) return null;
        return thanhToanRepository.findFirstByMaGiaoDichOrderByIdDesc(transactionRef).orElse(null);
    }

    private PhuongThucThanhToan findOrCreatePaymentMethod(String code, String name) {
        return paymentMethodRepository.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase(code, name)
                .orElseGet(() -> paymentMethodRepository.save(PhuongThucThanhToan.builder()
                        .maPttt(code)
                        .tenPttt(name)
                        .trangThai(true)
                        .build()));
    }

    private String paymentName(String gateway) {
        return switch (gateway) {
            case "BANKING" -> "Chuyển khoản QR mô phỏng";
            case "VNPAY" -> "VNPay";
            case "ZALOPAY" -> "Ví ZaloPay";
            default -> gateway;
        };
    }

    private String normalizeGateway(String value) {
        String gateway = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!gateway.equals("BANKING") && !gateway.equals("VNPAY") && !gateway.equals("ZALOPAY")) {
            throw new IllegalArgumentException("Cổng thanh toán không hợp lệ");
        }
        return gateway;
    }

    private long orderAmount(HoaDon order) {
        BigDecimal amount = order == null
                ? BigDecimal.ZERO
                : MoneyRoundingUtil.roundNonNegative(order.getTongTienThanhToan());
        return amount.longValueExact();
    }

    private boolean amountMatches(HoaDon order, long amount) {
        return order != null && orderAmount(order) == amount;
    }

    private boolean isExpiredVnPayCheckout(Map<String, String> params) {
        if (params == null) return true;
        String value = params.get("vnp_ExpireDate");
        if (value == null || value.isBlank()) return false;
        try {
            return LocalDateTime.parse(value, VNPAY_TIME).isBefore(LocalDateTime.now(VIETNAM_ZONE));
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private QrDemoPayment validateQrDemoPayment(Map<String, String> params) {
        if (!properties.getQrDemo().isEnabled() || params == null) return null;

        String gateway = text(params.get("gateway")).trim().toUpperCase(Locale.ROOT);
        String transactionRef = text(params.get("ref")).trim();
        long amount = parseLong(params.get("amount"));
        long expiresAt = parseLong(params.get("expires"));
        String receivedToken = text(params.get("token"));
        if ((!"BANKING".equals(gateway) && !"VNPAY".equals(gateway) && !"ZALOPAY".equals(gateway))
                || transactionRef.isBlank()
                || amount <= 0L
                || expiresAt <= System.currentTimeMillis() / 1_000L
                || !constantTimeEquals(qrDemoToken(gateway, transactionRef, amount, expiresAt), receivedToken)) {
            return null;
        }

        ThanhToan payment = findPayment(transactionRef);
        if (payment == null || payment.getHoaDon() == null
                || !amountMatches(payment.getHoaDon(), amount)) {
            return null;
        }

        PhuongThucThanhToan method = payment.getPhuongThucThanhToan();
        String paymentGateway = method == null ? "" : text(method.getMaPttt()).trim();
        if (!paymentGateway.isBlank() && !gateway.equalsIgnoreCase(paymentGateway)) {
            return null;
        }
        return new QrDemoPayment(payment, gateway);
    }

    private String qrDemoToken(String gateway, String transactionRef, long amount, long expiresAt) {
        return hmac(
                "HmacSHA256",
                qrDemoSecret(),
                gateway + "|" + transactionRef + "|" + amount + "|" + expiresAt
        );
    }

    private String qrDemoSecret() {
        String configured = properties.getQrDemo().getSecret();
        return configured == null || configured.isBlank() ? qrDemoFallbackSecret : configured;
    }

    private String transactionRef(String prefix, Integer orderId) {
        return prefix + orderId + "T" + System.currentTimeMillis();
    }

    private String callbackUrl(String path) {
        return properties.baseUrl() + path;
    }

    private String queryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String hmac(String algorithm, String key, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể ký dữ liệu thanh toán", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String received) {
        if (expected == null || received == null) return false;
        return MessageDigest.isEqual(
                expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJson(String endpoint, Map<String, Object> payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Cổng thanh toán phản hồi HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), Map.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kết nối cổng thanh toán bị gián đoạn", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException illegalStateException) throw illegalStateException;
            throw new IllegalStateException("Không thể kết nối cổng thanh toán", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForm(String endpoint, Map<String, String> payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(queryString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Cổng thanh toán phản hồi HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), Map.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kết nối cổng thanh toán bị gián đoạn", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException illegalStateException) throw illegalStateException;
            throw new IllegalStateException("Không thể kết nối cổng thanh toán", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Dữ liệu callback không hợp lệ", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo dữ liệu thanh toán", exception);
        }
    }

    private HttpClient httpClient() {
        HttpClient current = httpClient;
        if (current == null) {
            synchronized (this) {
                current = httpClient;
                if (current == null) {
                    current = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(10))
                            .build();
                    httpClient = current;
                }
            }
        }
        return current;
    }

    private long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        return parseLong(text(value));
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value == null || value.isBlank() ? "0" : value.trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record VnPayTestCard(String date, String cvv, String otp) {
    }

    private record QrDemoPayment(ThanhToan payment, String gateway) {
    }

    public record GatewayReturnResult(
            boolean valid,
            boolean success,
            boolean pending,
            String orderCode,
            String gateway
    ) {
    }

    public record LocalCheckoutStatus(boolean valid, boolean paid, String orderCode) {
    }

}
