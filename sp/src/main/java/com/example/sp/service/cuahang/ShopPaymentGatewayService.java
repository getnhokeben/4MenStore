package com.example.sp.service.cuahang;

import com.example.sp.config.PaymentGatewayProperties;
import com.example.sp.dto.cuahang.ShopPaymentResponse;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.HoaDonChiTiet;
import com.example.sp.model.hoadon.LichSuThanhToan;
import com.example.sp.model.hoadon.PhuongThucThanhToan;
import com.example.sp.model.hoadon.ThanhToan;
import com.example.sp.model.khuyenmai.DotGiamGia;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import com.example.sp.repository.hoadon.HoaDonChiTietRepository;
import com.example.sp.repository.hoadon.HoaDonRepository;
import com.example.sp.repository.hoadon.LichSuThanhToanRepository;
import com.example.sp.repository.hoadon.PhuongThucThanhToanRepository;
import com.example.sp.repository.hoadon.ThanhToanRepository;
import com.example.sp.repository.khuyenmai.PhieuGiamGiaRepository;
import com.example.sp.repository.khuyenmai.ChiTietDotGiamGiaRepository;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.service.tonkho.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper; // sửa import

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.List;
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
    private static final String LOCAL_DEMO_VNPAY_TMN_CODE = "LOCALDEMO";
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
    private final PhieuGiamGiaRepository voucherRepository;
    private final ChiTietDotGiamGiaRepository promotionDetailRepository;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile HttpClient httpClient;
    private final String qrDemoFallbackSecret = UUID.randomUUID().toString();

    // Thực hiện xử lý nghiệp vụ của hàm availability.
    public Map<String, Boolean> availability() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("COD", true);
        result.put("BANKING", properties.getQrDemo().isEnabled());
        // VNPay only becomes available when the real sandbox/production credentials exist.
        // Do not expose the local QR demo from the POS "Thẻ ATM / VNPay" payment option.
        result.put("VNPAY", properties.isVnPayConfigured());
        result.put("ZALOPAY", properties.getQrDemo().isEnabled() || properties.isZaloPayConfigured());
        return result;
    }

    @Transactional
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create payment.
    public ShopPaymentResponse createPayment(
            Integer orderId,
            String orderCode,
            String gatewayValue,
            String clientIp
    ) {
        HoaDon order = requirePayableOrder(orderId, orderCode);
        refreshOrderPricing(order);
        String gateway = normalizeGateway(gatewayValue);
        ensureGatewayConfigured(gateway);

        return switch (gateway) {
            case "BANKING" -> createQrDemoPayment(order, gateway);
            case "VNPAY" -> createVnPayPayment(order, clientIp);
            case "ZALOPAY" -> createZaloPayPayment(order);
            default -> throw new IllegalArgumentException("Cổng thanh toán không được hỗ trợ");
        };
    }

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create vn pay payment.
    private ShopPaymentResponse createVnPayPayment(HoaDon order, String clientIp) {
        PaymentGatewayProperties.VnPay config = properties.getVnpay();
        if (!properties.isVnPayConfigured() && !config.isLocalCheckoutEnabled()) {
            return createQrDemoPayment(order, "VNPAY");
        }
        boolean usingLocalDemo = !properties.isVnPayConfigured();
        String transactionRef = transactionRef("VP", order.getId());
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);

        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put(
                "vnp_TmnCode",
                usingLocalDemo ? LOCAL_DEMO_VNPAY_TMN_CODE : config.getTmnCode()
        );
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
        String signature = hmac(
                "HmacSHA512",
                usingLocalDemo ? qrDemoSecret() : config.getHashSecret(),
                query
        );
        String paymentEndpoint = usingLocalDemo || config.isLocalCheckoutEnabled()
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

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create momo payment.
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

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create zalo pay payment.
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

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create qr demo payment.
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
    // Xử lý tương tác người dùng cho handle vn pay ipn.
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
    // Thực hiện xử lý nghiệp vụ của hàm evaluate vn pay return.
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
    // Thực hiện xử lý nghiệp vụ của hàm complete local vn pay checkout.
    public GatewayReturnResult completeLocalVnPayCheckout(Map<String, String> params, boolean approved) {
        return completeLocalVnPayCheckout(params, approved, "LOCAL-");
    }

    // Thực hiện xử lý nghiệp vụ của hàm complete local vn pay checkout.
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
    // Thực hiện xử lý nghiệp vụ của hàm complete local vn pay wallet checkout.
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
    // Thực hiện xử lý nghiệp vụ của hàm confirm local vn pay qr scan.
    public GatewayReturnResult confirmLocalVnPayQrScan(Map<String, String> params) {
        if (isExpiredVnPayCheckout(params)) {
            return new GatewayReturnResult(false, false, false, "", "VNPAY");
        }
        return completeLocalVnPayCheckout(params, true, "LOCAL-QR-SCAN-");
    }

    @Transactional(readOnly = true)
    // Thực hiện xử lý nghiệp vụ của hàm local vn pay checkout status.
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
    // Thực hiện xử lý nghiệp vụ của hàm confirm qr demo scan.
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
    // Thực hiện xử lý nghiệp vụ của hàm qr demo checkout status.
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
    // Thực hiện xử lý nghiệp vụ của hàm complete local vn pay test checkout.
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
    // Xử lý tương tác người dùng cho handle momo ipn.
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
    // Thực hiện xử lý nghiệp vụ của hàm evaluate momo return.
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
    // Xử lý tương tác người dùng cho handle zalo pay callback.
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
    // Thực hiện xử lý nghiệp vụ của hàm evaluate zalo pay return.
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

    // Kiểm tra điều kiện và tính hợp lệ cho verify zalo pay redirect.
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

    // Thực hiện xử lý nghiệp vụ của hàm query zalo pay order.
    private Map<String, Object> queryZaloPayOrder(String transactionRef) {
        PaymentGatewayProperties.ZaloPay config = properties.getZalopay();
        String macInput = config.getAppId() + "|" + transactionRef + "|" + config.getKey1();
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("app_id", String.valueOf(config.getAppId()));
        payload.put("app_trans_id", transactionRef);
        payload.put("mac", hmac("HmacSHA256", config.getKey1(), macInput));
        return postForm(config.getQueryEndpoint(), payload);
    }

    // Kiểm tra điều kiện và tính hợp lệ cho verify vn pay signature.
    private boolean verifyVnPaySignature(Map<String, String> source) {
        if (source == null) return false;

        boolean usingLocalDemo = !properties.isVnPayConfigured();
        if (usingLocalDemo && !properties.getQrDemo().isEnabled()) return false;

        String expectedTmnCode = usingLocalDemo
                ? LOCAL_DEMO_VNPAY_TMN_CODE
                : properties.getVnpay().getTmnCode();
        String signingSecret = usingLocalDemo
                ? qrDemoSecret()
                : properties.getVnpay().getHashSecret();
        if (!expectedTmnCode.equals(source.get("vnp_TmnCode"))) return false;
        String received = source.get("vnp_SecureHash");
        TreeMap<String, String> signed = new TreeMap<>();
        source.forEach((key, value) -> {
            if (key != null && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key) && !"vnp_SecureHashType".equals(key)
                    && value != null && !value.isBlank()) {
                signed.put(key, value);
            }
        });
        String expected = hmac("HmacSHA512", signingSecret, queryString(signed));
        return constantTimeEquals(expected, received);
    }

    // Kiểm tra điều kiện và tính hợp lệ cho verify momo signature.
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

    // Thực hiện xử lý nghiệp vụ của hàm return result.
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

    // Thực hiện xử lý nghiệp vụ của hàm require payable order.
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

    /** Recalculate campaigns and vouchers at the final payment boundary. */
    // Thực hiện xử lý nghiệp vụ của hàm refresh order pricing.
    private void refreshOrderPricing(HoaDon order) {
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(order.getId());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn chưa có sản phẩm để thanh toán");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (HoaDonChiTiet item : items) {
            if (item.getChiTietSanPham() == null || item.getChiTietSanPham().getIdSpct() == null) {
                throw new IllegalStateException("Không tìm thấy biến thể sản phẩm của hóa đơn");
            }
            int quantity = item.getSoLuong() == null ? 0 : item.getSoLuong();
            BigDecimal original = MoneyRoundingUtil.roundNonNegative(item.getChiTietSanPham().getDonGia());
            BigDecimal currentPrice = promotionDetailRepository
                    .findActivePromotionsByVariantId(item.getChiTietSanPham().getIdSpct(), LocalDateTime.now())
                    .stream()
                    .map(promotion -> campaignPrice(original, promotion))
                    .min(BigDecimal::compareTo)
                    .orElse(original);
            item.setDonGia(currentPrice);
            item.setThanhTien(MoneyRoundingUtil.roundNonNegative(
                    currentPrice.multiply(BigDecimal.valueOf(quantity))));
            subtotal = subtotal.add(item.getThanhTien());
        }
        hoaDonChiTietRepository.saveAll(items);

        PhieuGiamGia selectedVoucher = order.getPhieuGiamGia();
        BigDecimal discount = BigDecimal.ZERO;
        if (selectedVoucher != null && selectedVoucher.getId() != null) {
            PhieuGiamGia voucher = voucherRepository.findByIdForUpdate(selectedVoucher.getId()).orElse(null);
            // An online/POS draft can reserve a voucher usage before reaching this
            // gateway. That reservation belongs to this order, so reaching the
            // quantity limit must not incorrectly remove its own voucher.
            if (isVoucherUsable(voucher, subtotal, true)) {
                order.setPhieuGiamGia(voucher);
                discount = voucherDiscount(voucher, subtotal);
            } else {
                order.setPhieuGiamGia(null);
            }
        }

        order.setTongTienGoc(MoneyRoundingUtil.roundNonNegative(subtotal));
        order.setSoTienGiam(discount);
        BigDecimal shippingFee = MoneyRoundingUtil.roundNonNegative(order.getPhiVanChuyen());
        order.setPhiVanChuyen(shippingFee);
        order.setTongTienThanhToan(MoneyRoundingUtil.roundNonNegative(
                subtotal.subtract(discount).add(shippingFee)));
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepository.save(order);
    }

    // Thực hiện xử lý nghiệp vụ của hàm campaign price.
    private BigDecimal campaignPrice(BigDecimal original, DotGiamGia campaign) {
        BigDecimal value = "PHAN_TRAM".equalsIgnoreCase(campaign.getLoaiGiamGia())
                ? money(campaign.getGiaTriGiamGia())
                : MoneyRoundingUtil.roundNonNegative(campaign.getGiaTriGiamGia());
        if (value.compareTo(BigDecimal.ZERO) <= 0) return original;
        boolean percent = "PHAN_TRAM".equalsIgnoreCase(campaign.getLoaiGiamGia());
        BigDecimal reduction = percent
                ? original.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : value;
        if (percent && campaign.getSoTienToiDa() != null && campaign.getSoTienToiDa().compareTo(BigDecimal.ZERO) > 0) {
            reduction = reduction.min(MoneyRoundingUtil.roundNonNegative(campaign.getSoTienToiDa()));
        }
        return MoneyRoundingUtil.roundNonNegative(original.subtract(reduction));
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is voucher usable.
    private boolean isVoucherUsable(PhieuGiamGia voucher, BigDecimal subtotal, boolean usageAlreadyReserved) {
        if (voucher == null || !Boolean.TRUE.equals(voucher.getTrangThai())) return false;
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() != null && voucher.getNgayBatDau().isAfter(now)) return false;
        if (voucher.getNgayKetThuc() != null && voucher.getNgayKetThuc().isBefore(now)) return false;
        int used = voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung();
        if (voucher.getSoLuong() != null && used >= voucher.getSoLuong() && !usageAlreadyReserved) return false;
        return subtotal.compareTo(MoneyRoundingUtil.roundNonNegative(voucher.getDieuKienDonHang())) >= 0;
    }

    // Thực hiện xử lý nghiệp vụ của hàm voucher discount.
    private BigDecimal voucherDiscount(PhieuGiamGia voucher, BigDecimal subtotal) {
        boolean percent = normalize(voucher.getLoaiGiam()).contains("phan") || normalize(voucher.getLoaiGiam()).contains("%");
        BigDecimal value = percent ? money(voucher.getGiaTri()) : MoneyRoundingUtil.roundNonNegative(voucher.getGiaTri());
        BigDecimal discount = percent
                ? subtotal.multiply(value).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : value;
        BigDecimal maximum = MoneyRoundingUtil.roundNonNegative(voucher.getGiaTriToiDa());
        if (maximum.compareTo(BigDecimal.ZERO) > 0) discount = discount.min(maximum);
        return MoneyRoundingUtil.roundNonNegative(discount.min(subtotal));
    }

    // Thực hiện xử lý nghiệp vụ của hàm money.
    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize.
    private String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    // Kiểm tra điều kiện và tính hợp lệ cho is pos order.
    public boolean isPosOrder(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) return false;
        return hoaDonRepository.findFirstByMaHoaDonIgnoreCase(orderCode.trim())
                .map(this::isPosOrder)
                .orElse(false);
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is pos order.
    private boolean isPosOrder(HoaDon order) {
        return order != null && "Tại quầy".equalsIgnoreCase(order.getLoaiDon());
    }

    // Thực hiện xử lý nghiệp vụ của hàm ensure order gateway.
    private void ensureOrderGateway(HoaDon order, String gateway) {
        String note = order.getGhiChu() == null ? "" : order.getGhiChu().toUpperCase(Locale.ROOT);
        if (!note.contains("PHƯƠNG THỨC THANH TOÁN: " + gateway)) {
            throw new IllegalArgumentException("Phương thức thanh toán không khớp với đơn hàng");
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm ensure gateway configured.
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

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create pending payment.
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

    // Thực hiện xử lý nghiệp vụ của hàm mark paid.
    private void markPaid(ThanhToan payment, String gateway, String externalTransactionId) {
        if (payment == null || payment.getHoaDon() == null) return;
        if (isPaymentSuccessful(payment)) return;

        LocalDateTime paidAt = LocalDateTime.now();
        HoaDon order = payment.getHoaDon();
        // A failed gateway attempt cancels the pending order and releases its
        // voucher. A delayed success callback must not resurrect that order.
        if (!"Chờ thanh toán online".equalsIgnoreCase(order.getTrangThai())) return;
        payment.setTrangThai("Thành công");
        payment.setThoiGianThanhToan(paidAt);
        thanhToanRepository.save(payment);

        // A customer may retry and create more than one valid gateway transaction.
        // The first successful callback owns the order; later callbacks stay idempotent.
        if (order.getNgayThanhToan() != null) return;

        // The gateway confirmation moves the order to the confirmed state first;
        // stock deduction belongs exclusively to this state transition.
        boolean counterPickup = isPosOrder(order)
                && "Tại quầy".equalsIgnoreCase(order.getHinhThucNhanHang());
        order.setTrangThai(counterPickup ? "Hoàn thành" : "Đã xác nhận");
        order.captureVoucherSnapshot();
        confirmPaidOrderStock(order);
        order.setNgayThanhToan(paidAt);
        order.setNgayCapNhat(paidAt);
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

    // Thực hiện xử lý nghiệp vụ của hàm confirm paid order stock.
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

    // Thực hiện xử lý nghiệp vụ của hàm mark failed.
    private void markFailed(ThanhToan payment) {
        if (payment == null || isPaymentSuccessful(payment)
                || "Thất bại".equalsIgnoreCase(payment.getTrangThai())) {
            return;
        }
        payment.setTrangThai("Thất bại");
        thanhToanRepository.save(payment);

        HoaDon order = payment.getHoaDon();
        if (order == null || !"Chờ thanh toán online".equalsIgnoreCase(order.getTrangThai())) {
            return;
        }

        releaseVoucherUsage(order);
        if (isPosOrder(order) && Boolean.TRUE.equals(order.getDaTruTon())) {
            hoaDonChiTietRepository.findByHoaDon_Id(order.getId()).forEach(item -> {
                int quantity = item.getSoLuong() == null ? 0 : item.getSoLuong();
                if (item.getChiTietSanPham() != null && item.getChiTietSanPham().getIdSpct() != null
                        && quantity > 0) {
                    inventoryService.restoreStock(item.getChiTietSanPham().getIdSpct(), quantity);
                }
            });
            order.setDaTruTon(false);
        }
        order.setTrangThai("Đã hủy");
        order.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepository.save(order);
    }

    // Thực hiện xử lý nghiệp vụ của hàm release voucher usage.
    private void releaseVoucherUsage(HoaDon order) {
        if (order.getPhieuGiamGia() == null || order.getPhieuGiamGia().getId() == null) {
            return;
        }
        voucherRepository.findByIdForUpdate(order.getPhieuGiamGia().getId())
                .ifPresent(voucher -> {
                    int used = voucher.getSoLuongDaDung() == null ? 0 : voucher.getSoLuongDaDung();
                    if (used > 0) {
                        voucher.setSoLuongDaDung(used - 1);
                        voucherRepository.save(voucher);
                    }
                });
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is payment successful.
    private boolean isPaymentSuccessful(ThanhToan payment) {
        return payment != null
                && payment.getHoaDon() != null
                && payment.getHoaDon().getNgayThanhToan() != null;
    }

    // Tải hoặc truy xuất dữ liệu cho find payment.
    private ThanhToan findPayment(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) return null;
        return thanhToanRepository.findFirstByMaGiaoDichOrderByIdDesc(transactionRef).orElse(null);
    }

    // Tải hoặc truy xuất dữ liệu cho find or create payment method.
    private PhuongThucThanhToan findOrCreatePaymentMethod(String code, String name) {
        return paymentMethodRepository.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase(code, name)
                .orElseGet(() -> paymentMethodRepository.save(PhuongThucThanhToan.builder()
                        .maPttt(code)
                        .tenPttt(name)
                        .trangThai(true)
                        .build()));
    }

    // Thực hiện xử lý nghiệp vụ của hàm payment name.
    private String paymentName(String gateway) {
        return switch (gateway) {
            case "BANKING" -> "Chuyển khoản QR mô phỏng";
            case "VNPAY" -> "VNPay";
            case "ZALOPAY" -> "Ví ZaloPay";
            default -> gateway;
        };
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize gateway.
    private String normalizeGateway(String value) {
        String gateway = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!gateway.equals("BANKING") && !gateway.equals("VNPAY") && !gateway.equals("ZALOPAY")) {
            throw new IllegalArgumentException("Cổng thanh toán không hợp lệ");
        }
        return gateway;
    }

    // Thực hiện xử lý nghiệp vụ của hàm order amount.
    private long orderAmount(HoaDon order) {
        BigDecimal amount = order == null
                ? BigDecimal.ZERO
                : MoneyRoundingUtil.roundNonNegative(order.getTongTienThanhToan());
        return amount.longValueExact();
    }

    // Thực hiện xử lý nghiệp vụ của hàm amount matches.
    private boolean amountMatches(HoaDon order, long amount) {
        return order != null && orderAmount(order) == amount;
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is expired vn pay checkout.
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

    // Kiểm tra điều kiện và tính hợp lệ cho validate qr demo payment.
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

    // Thực hiện xử lý nghiệp vụ của hàm qr demo token.
    private String qrDemoToken(String gateway, String transactionRef, long amount, long expiresAt) {
        return hmac(
                "HmacSHA256",
                qrDemoSecret(),
                gateway + "|" + transactionRef + "|" + amount + "|" + expiresAt
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm qr demo secret.
    private String qrDemoSecret() {
        String configured = properties.getQrDemo().getSecret();
        return configured == null || configured.isBlank() ? qrDemoFallbackSecret : configured;
    }

    // Thực hiện xử lý nghiệp vụ của hàm transaction ref.
    private String transactionRef(String prefix, Integer orderId) {
        return prefix + orderId + "T" + System.currentTimeMillis();
    }

    // Thực hiện xử lý nghiệp vụ của hàm callback url.
    private String callbackUrl(String path) {
        return properties.baseUrl() + path;
    }

    // Thực hiện xử lý nghiệp vụ của hàm query string.
    private String queryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    // Thực hiện xử lý nghiệp vụ của hàm url encode.
    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    // Thực hiện xử lý nghiệp vụ của hàm hmac.
    private String hmac(String algorithm, String key, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể ký dữ liệu thanh toán", exception);
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm constant time equals.
    private boolean constantTimeEquals(String expected, String received) {
        if (expected == null || received == null) return false;
        return MessageDigest.isEqual(
                expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm to hex.
    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    // Thực hiện xử lý nghiệp vụ của hàm post json.
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
    // Thực hiện xử lý nghiệp vụ của hàm post form.
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
    // Tải hoặc truy xuất dữ liệu cho read json.
    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Dữ liệu callback không hợp lệ", exception);
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm write json.
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo dữ liệu thanh toán", exception);
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm http client.
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

    // Thực hiện xử lý nghiệp vụ của hàm number.
    private long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        return parseLong(text(value));
    }

    // Thực hiện xử lý nghiệp vụ của hàm parse long.
    private long parseLong(String value) {
        try {
            return Long.parseLong(value == null || value.isBlank() ? "0" : value.trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm text.
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    // Thực hiện xử lý nghiệp vụ của hàm vn pay test card.
    private record VnPayTestCard(String date, String cvv, String otp) {
    }

    // Thực hiện xử lý nghiệp vụ của hàm qr demo payment.
    private record QrDemoPayment(ThanhToan payment, String gateway) {
    }

    // Thực hiện xử lý nghiệp vụ của hàm gateway return result.
    public record GatewayReturnResult(
            boolean valid,
            boolean success,
            boolean pending,
            String orderCode,
            String gateway
    ) {
    }

    // Thực hiện xử lý nghiệp vụ của hàm local checkout status.
    public record LocalCheckoutStatus(boolean valid, boolean paid, String orderCode) {
    }

}
