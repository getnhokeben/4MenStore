package com.example.sp.service.cuahang;

import com.example.sp.config.PaymentGatewayProperties;
import com.example.sp.dto.cuahang.ShopPaymentResponse;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.HoaDonChiTiet;
import com.example.sp.model.hoadon.LichSuThanhToan;
import com.example.sp.model.hoadon.PhuongThucThanhToan;
import com.example.sp.model.hoadon.ThanhToan;
import com.example.sp.model.khuyenmai.PhieuGiamGia;
import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.repository.hoadon.HoaDonChiTietRepository;
import com.example.sp.repository.hoadon.HoaDonRepository;
import com.example.sp.repository.hoadon.LichSuThanhToanRepository;
import com.example.sp.repository.hoadon.PhuongThucThanhToanRepository;
import com.example.sp.repository.hoadon.ThanhToanRepository;
import com.example.sp.repository.khuyenmai.PhieuGiamGiaRepository;
import com.example.sp.repository.khuyenmai.ChiTietDotGiamGiaRepository;
import com.example.sp.service.tonkho.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import com.sun.net.httpserver.HttpServer;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ShopPaymentGatewayServiceTest {

    private static final String TEST_TMN_CODE = "TESTCODE";
    private static final String TEST_SECRET = "test-only-vnpay-secret";
    private static final int TEST_ZALOPAY_APP_ID = 15847;
    private static final String TEST_ZALOPAY_KEY1 = "test-only-zalopay-key1";
    private static final String TEST_ZALOPAY_KEY2 = "test-only-zalopay-key2";

    private final PaymentGatewayProperties properties = new PaymentGatewayProperties();
    private final HoaDonRepository orderRepository = mock(HoaDonRepository.class);
    private final HoaDonChiTietRepository orderItemRepository = mock(HoaDonChiTietRepository.class);
    private final ThanhToanRepository paymentRepository = mock(ThanhToanRepository.class);
    private final PhuongThucThanhToanRepository methodRepository = mock(PhuongThucThanhToanRepository.class);
    private final LichSuThanhToanRepository historyRepository = mock(LichSuThanhToanRepository.class);
    private final PhieuGiamGiaRepository voucherRepository = mock(PhieuGiamGiaRepository.class);
    private final ChiTietDotGiamGiaRepository promotionDetailRepository = mock(ChiTietDotGiamGiaRepository.class);
    private final InventoryService inventoryService = mock(InventoryService.class);
    private ShopPaymentGatewayService service;

    @BeforeEach
    void setUp() {
        properties.setPublicBaseUrl("https://shop.example");
        properties.getVnpay().setEnabled(true);
        properties.getVnpay().setLocalCheckoutEnabled(false);
        properties.getVnpay().setTmnCode(TEST_TMN_CODE);
        properties.getVnpay().setHashSecret(TEST_SECRET);
        properties.getVnpay().setPaymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        service = new ShopPaymentGatewayService(
                properties,
                orderRepository,
                orderItemRepository,
                paymentRepository,
                methodRepository,
                historyRepository,
                voucherRepository,
                promotionDetailRepository,
                inventoryService
        );
        ChiTietSanPham defaultVariant = new ChiTietSanPham();
        defaultVariant.setIdSpct(101);
        defaultVariant.setDonGia(new BigDecimal("250000"));
        HoaDonChiTiet defaultItem = HoaDonChiTiet.builder()
                .chiTietSanPham(defaultVariant)
                .soLuong(1)
                .build();
        when(orderItemRepository.findByHoaDon_Id(42)).thenReturn(List.of(defaultItem));
    }

    @Test
    void createsSignedSandboxPaymentUrl() {
        HoaDon order = payableOrder();
        PhuongThucThanhToan method = PhuongThucThanhToan.builder()
                .id(1).maPttt("VNPAY").tenPttt("VNPay").trangThai(true).build();
        when(orderRepository.findById(42)).thenReturn(Optional.of(order));
        when(methodRepository.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase("VNPAY", "VNPay"))
                .thenReturn(Optional.of(method));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopPaymentResponse response = service.createPayment(42, "HD42", "VNPAY", "127.0.0.1");

        String rawQuery = response.getPaymentUrl().substring(response.getPaymentUrl().indexOf('?') + 1);
        int signatureIndex = rawQuery.indexOf("&vnp_SecureHash=");
        String signedData = rawQuery.substring(0, signatureIndex);
        String receivedSignature = rawQuery.substring(signatureIndex + "&vnp_SecureHash=".length());

        assertEquals(hmacSha512(TEST_SECRET, signedData), receivedSignature);
        assertTrue(signedData.contains("vnp_Amount=25000000"));
        assertTrue(signedData.contains("vnp_TmnCode=" + TEST_TMN_CODE));
        assertTrue(signedData.contains("vnp_ReturnUrl=https%3A%2F%2Fshop.example%2Fapi%2Fshop%2Fpayments%2Fvnpay%2Freturn"));
    }

    @Test
    void roundsVnPayAmountToNearestThousand() {
        HoaDon order = payableOrder();
        order.setTongTienThanhToan(new BigDecimal("250500"));
        ChiTietSanPham variant = new ChiTietSanPham();
        variant.setIdSpct(101);
        variant.setDonGia(new BigDecimal("250500"));
        when(orderItemRepository.findByHoaDon_Id(42)).thenReturn(List.of(
                HoaDonChiTiet.builder().chiTietSanPham(variant).soLuong(1).build()
        ));
        PhuongThucThanhToan method = PhuongThucThanhToan.builder()
                .id(1).maPttt("VNPAY").tenPttt("VNPay").trangThai(true).build();
        when(orderRepository.findById(42)).thenReturn(Optional.of(order));
        when(methodRepository.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase("VNPAY", "VNPay"))
                .thenReturn(Optional.of(method));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopPaymentResponse response = service.createPayment(42, "HD42", "VNPAY", "127.0.0.1");

        assertTrue(response.getPaymentUrl().contains("vnp_Amount=25100000"));
    }

    @Test
    void routesVnPayThroughLocalCheckoutWhenEnabled() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        PhuongThucThanhToan method = PhuongThucThanhToan.builder()
                .id(1).maPttt("VNPAY").tenPttt("VNPay").trangThai(true).build();
        when(orderRepository.findById(42)).thenReturn(Optional.of(order));
        when(methodRepository.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase("VNPAY", "VNPay"))
                .thenReturn(Optional.of(method));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopPaymentResponse response = service.createPayment(42, "HD42", "VNPAY", "127.0.0.1");

        assertTrue(response.getPaymentUrl().startsWith(
                "https://shop.example/api/shop/payments/vnpay/checkout?"
        ));
        assertTrue(response.getPaymentUrl().contains("vnp_SecureHash="));
    }

    @Test
    void routesDemoVnPayToCardCheckoutWhenSandboxCredentialsAreUnavailable() {
        properties.getVnpay().setEnabled(false);
        properties.getVnpay().setTmnCode("");
        properties.getVnpay().setHashSecret("");
        properties.getVnpay().setLocalCheckoutEnabled(true);
        properties.getQrDemo().setEnabled(true);

        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(13)
                .hoaDon(order)
                .maGiaoDich("VP42TDEMO")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chá» thanh toÃ¡n")
                .build();
        PhuongThucThanhToan method = PhuongThucThanhToan.builder()
                .id(1).maPttt("VNPAY").tenPttt("VNPay").trangThai(true).build();
        when(orderRepository.findById(42)).thenReturn(Optional.of(order));
        when(methodRepository.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase("VNPAY", "VNPay"))
                .thenReturn(Optional.of(method));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc(any()))
                .thenReturn(Optional.of(payment));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopPaymentResponse response = service.createPayment(42, "HD42", "VNPAY", "127.0.0.1");
        Map<String, String> params = queryParams(response.getPaymentUrl());

        assertTrue(response.getPaymentUrl().startsWith(
                "https://shop.example/api/shop/payments/vnpay/checkout?"
        ));
        assertEquals("LOCALDEMO", params.get("vnp_TmnCode"));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayTestCheckout(
                        params, "pay", "9704198526191432198", "07/15", "", "123456"
                );

        assertTrue(result.valid());
        assertTrue(result.success());
        assertNotNull(order.getNgayThanhToan());
    }

    @Test
    void completesSignedLocalCheckoutAndMarksOrderPaid() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(12)
                .hoaDon(order)
                .maGiaoDich("VP42TLOCAL")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TLOCAL"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TLOCAL");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ReturnUrl", "https://shop.example/api/shop/payments/vnpay/return");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayTestCheckout(
                        params, "pay", "9704198526191432198", "07/15", "", "123456"
                );

        assertTrue(result.valid());
        assertTrue(result.success());
        assertFalse(result.pending());
        assertEquals("HD42", result.orderCode());
        assertNotNull(order.getNgayThanhToan());
    }

    @Test
    void completesNo3dsVisaTestCardWithoutOtp() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(15)
                .hoaDon(order)
                .maGiaoDich("VP42TVISA")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TVISA"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TVISA");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayTestCheckout(
                        params, "pay", "4456530000001005", "12/26", "123", ""
                );

        assertTrue(result.valid());
        assertTrue(result.success());
        assertNotNull(order.getNgayThanhToan());
    }

    @Test
    void successfulGatewayPaymentConvertsReservationIntoDeductedStock() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        order.setDaGiuTon(true);
        order.setDaTruTon(false);
        ThanhToan payment = ThanhToan.builder()
                .id(16)
                .hoaDon(order)
                .maGiaoDich("VP42TSTOCK")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        ChiTietSanPham variant = new ChiTietSanPham();
        variant.setIdSpct(101);
        HoaDonChiTiet item = HoaDonChiTiet.builder()
                .hoaDon(order)
                .chiTietSanPham(variant)
                .soLuong(2)
                .build();

        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TSTOCK"))
                .thenReturn(Optional.of(payment));
        when(orderItemRepository.findByHoaDon_Id(42)).thenReturn(List.of(item));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TSTOCK");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayTestCheckout(
                        params, "pay", "4456530000001005", "12/26", "123", ""
                );

        assertTrue(result.success());
        verify(inventoryService).confirmOnlineReservation(101, 2);
        assertFalse(Boolean.TRUE.equals(order.getDaGiuTon()));
        assertTrue(Boolean.TRUE.equals(order.getDaTruTon()));
        assertEquals("Đã xác nhận", order.getTrangThai());
    }

    @Test
    void successfulGatewayPaymentDeductsUnreservedStockWhenOrderBecomesConfirmed() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        order.setDaGiuTon(false);
        order.setDaTruTon(false);
        ThanhToan payment = ThanhToan.builder()
                .id(26)
                .hoaDon(order)
                .maGiaoDich("VP42TUNRESERVED")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        ChiTietSanPham variant = new ChiTietSanPham();
        variant.setIdSpct(101);
        HoaDonChiTiet item = HoaDonChiTiet.builder()
                .hoaDon(order)
                .chiTietSanPham(variant)
                .soLuong(2)
                .build();

        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TUNRESERVED"))
                .thenReturn(Optional.of(payment));
        when(orderItemRepository.findByHoaDon_Id(42)).thenReturn(List.of(item));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TUNRESERVED");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayTestCheckout(
                        params, "pay", "4456530000001005", "12/26", "123", ""
                );

        assertTrue(result.success());
        assertEquals("Đã xác nhận", order.getTrangThai());
        verify(inventoryService).deductOnlineStock(101, 2);
        assertFalse(Boolean.TRUE.equals(order.getDaGiuTon()));
        assertTrue(Boolean.TRUE.equals(order.getDaTruTon()));
    }

    @Test
    void completesSignedVnPayQrCheckoutAndWritesPaymentHistory() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(17)
                .hoaDon(order)
                .maGiaoDich("VP42TQR")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TQR"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(LichSuThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TQR");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayWalletCheckout(params, "pay", "QR");

        assertTrue(result.valid());
        assertTrue(result.success());
        assertEquals("Thành công", payment.getTrangThai());
        assertNotNull(order.getNgayThanhToan());
        ArgumentCaptor<LichSuThanhToan> historyCaptor = ArgumentCaptor.forClass(LichSuThanhToan.class);
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals("VNPAY-LOCAL-QR-VP42TQR", historyCaptor.getValue().getMaGiaoDich());
        assertEquals("VNPay", historyCaptor.getValue().getHinhThucThanhToan());
    }

    @Test
    void reportsPendingUntilSignedQrScanConfirmsPayment() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(19)
                .hoaDon(order)
                .maGiaoDich("VP42TQRSCAN")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TQRSCAN"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TQRSCAN");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ExpireDate", "29991231235959");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.LocalCheckoutStatus before =
                service.localVnPayCheckoutStatus(params);
        ShopPaymentGatewayService.GatewayReturnResult confirmation =
                service.confirmLocalVnPayQrScan(params);
        ShopPaymentGatewayService.LocalCheckoutStatus after =
                service.localVnPayCheckoutStatus(params);

        assertTrue(before.valid());
        assertFalse(before.paid());
        assertTrue(confirmation.valid());
        assertTrue(confirmation.success());
        assertTrue(after.valid());
        assertTrue(after.paid());
        assertEquals("HD42", after.orderCode());
    }

    @Test
    void rejectsExpiredSignedQrScan() {
        properties.getVnpay().setLocalCheckoutEnabled(true);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TEXPIRED");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ExpireDate", "20000101000000");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.confirmLocalVnPayQrScan(params);

        assertFalse(result.valid());
        assertFalse(result.success());
    }

    @Test
    void confirmsBankingQrDemoAfterPhoneScan() {
        properties.getQrDemo().setEnabled(true);
        properties.getQrDemo().setSecret("test-only-qr-demo-secret");
        HoaDon order = payableOrder();
        order.setGhiChu("Phương thức thanh toán: BANKING");
        PhuongThucThanhToan method = PhuongThucThanhToan.builder()
                .id(31).maPttt("BANKING").tenPttt("Chuyển khoản QR mô phỏng").trangThai(true).build();
        when(orderRepository.findById(42)).thenReturn(Optional.of(order));
        when(methodRepository.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase(
                "BANKING", "Chuyển khoản QR mô phỏng"
        )).thenReturn(Optional.of(method));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopPaymentResponse response = service.createPayment(42, "HD42", "BANKING", "127.0.0.1");
        Map<String, String> params = queryParams(response.getPaymentUrl());
        ArgumentCaptor<ThanhToan> paymentCaptor = ArgumentCaptor.forClass(ThanhToan.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        ThanhToan payment = paymentCaptor.getValue();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc(response.getTransactionRef()))
                .thenReturn(Optional.of(payment));

        ShopPaymentGatewayService.LocalCheckoutStatus before = service.qrDemoCheckoutStatus(params);
        ShopPaymentGatewayService.GatewayReturnResult confirmation = service.confirmQrDemoScan(params);
        ShopPaymentGatewayService.LocalCheckoutStatus after = service.qrDemoCheckoutStatus(params);

        assertTrue(response.getPaymentUrl().startsWith(
                "https://shop.example/api/shop/payments/qr-demo/checkout?"
        ));
        assertEquals("BANKING", params.get("gateway"));
        assertTrue(before.valid());
        assertFalse(before.paid());
        assertTrue(confirmation.success());
        assertTrue(after.paid());
        assertNotNull(order.getNgayThanhToan());
    }

    @Test
    void routesZaloPayToQrDemoWithoutCallingZaloPaySandbox() {
        properties.getQrDemo().setEnabled(true);
        properties.getQrDemo().setSecret("test-only-qr-demo-secret");
        HoaDon order = payableOrder();
        order.setGhiChu("Phương thức thanh toán: ZALOPAY");
        PhuongThucThanhToan method = PhuongThucThanhToan.builder()
                .id(32).maPttt("ZALOPAY").tenPttt("Ví ZaloPay").trangThai(true).build();
        when(orderRepository.findById(42)).thenReturn(Optional.of(order));
        when(methodRepository.findFirstByMaPtttIgnoreCaseOrTenPtttIgnoreCase("ZALOPAY", "Ví ZaloPay"))
                .thenReturn(Optional.of(method));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopPaymentResponse response = service.createPayment(42, "HD42", "ZALOPAY", "127.0.0.1");

        assertEquals("ZALOPAY", response.getGateway());
        assertTrue(response.getPaymentUrl().startsWith(
                "https://shop.example/api/shop/payments/qr-demo/checkout?"
        ));
        assertEquals("ZALOPAY", queryParams(response.getPaymentUrl()).get("gateway"));
    }

    @Test
    void completesSignedVnPayAppCheckoutAndMarksOrderPaid() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(18)
                .hoaDon(order)
                .maGiaoDich("VP42TAPP")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TAPP"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TAPP");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayWalletCheckout(params, "pay", "APP");

        assertTrue(result.valid());
        assertTrue(result.success());
        assertEquals("Thành công", payment.getTrangThai());
        assertNotNull(order.getNgayThanhToan());
    }

    @Test
    void declinesInsufficientBalanceNcbTestCard() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setId(9);
        voucher.setSoLuongDaDung(1);
        order.setPhieuGiamGia(voucher);
        ThanhToan payment = ThanhToan.builder()
                .id(16)
                .hoaDon(order)
                .maGiaoDich("VP42TDECLINED")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TDECLINED"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(voucherRepository.findByIdForUpdate(9)).thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(PhieuGiamGia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TDECLINED");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayTestCheckout(
                        params, "pay", "9704195798459170488", "07/15", "", ""
                );

        assertTrue(result.valid());
        assertFalse(result.success());
        assertNull(order.getNgayThanhToan());
        assertEquals("Thất bại", payment.getTrangThai());
        assertEquals("Đã hủy", order.getTrangThai());
        assertEquals(0, voucher.getSoLuongDaDung());

        service.completeLocalVnPayTestCheckout(
                params, "pay", "9704198526191432198", "07/15", "", "123456"
        );

        assertNull(order.getNgayThanhToan());
        assertEquals("Đã hủy", order.getTrangThai());
        assertEquals("Thất bại", payment.getTrangThai());
    }

    @Test
    void rejectsTamperedLocalCheckoutAmount() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(13)
                .hoaDon(order)
                .maGiaoDich("VP42TTAMPERED")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TTAMPERED"))
                .thenReturn(Optional.of(payment));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TTAMPERED");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_SecureHash", sign(params));
        params.put("vnp_Amount", "10000");

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayTestCheckout(
                        params, "pay", "9704198526191432198", "07/15", "", "123456"
                );

        assertFalse(result.valid());
        assertFalse(result.success());
        assertNull(order.getNgayThanhToan());
    }

    @Test
    void cancelsSignedLocalCheckoutWithoutMarkingOrderPaid() {
        properties.getVnpay().setLocalCheckoutEnabled(true);
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(14)
                .hoaDon(order)
                .maGiaoDich("VP42TCANCEL")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42TCANCEL"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42TCANCEL");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_SecureHash", sign(params));

        ShopPaymentGatewayService.GatewayReturnResult result =
                service.completeLocalVnPayTestCheckout(
                        params, "cancel", "9704198526191432198", "", "", ""
                );

        assertTrue(result.valid());
        assertFalse(result.success());
        assertFalse(result.pending());
        assertNull(order.getNgayThanhToan());
    }

    @Test
    void acceptsValidIpnAndMarksOrderPaid() {
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(9)
                .hoaDon(order)
                .maGiaoDich("VP42T1")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42T1"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.existsByMaGiaoDich("VNPAY-12345")).thenReturn(false);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", "VP42T1");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TransactionNo", "12345");
        params.put("vnp_SecureHash", sign(params));

        Map<String, String> result = service.handleVnPayIpn(params);

        assertEquals("00", result.get("RspCode"));
        assertEquals("Thành công", payment.getTrangThai());
        assertEquals("Đã xác nhận", order.getTrangThai());
        assertNotNull(order.getNgayThanhToan());
    }

    @Test
    void rejectsCallbackFromAnotherTerminalEvenWithValidHashSecret() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", "OTHERCODE");
        params.put("vnp_TxnRef", "VP42T1");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_SecureHash", sign(params));

        assertEquals("97", service.handleVnPayIpn(params).get("RspCode"));
    }

    @Test
    void reconcilesApprovedReturnOnlyWhenExplicitlyEnabled() {
        properties.getVnpay().setReconcileOnReturn(true);
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(10)
                .hoaDon(order)
                .maGiaoDich("VP42T2")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chá» thanh toÃ¡n")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42T2"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.existsByMaGiaoDich("VNPAY-67890")).thenReturn(false);

        Map<String, String> params = approvedReturn("VP42T2", "67890");

        ShopPaymentGatewayService.GatewayReturnResult result = service.evaluateVnPayReturn(params);

        assertTrue(result.valid());
        assertTrue(result.success());
        assertFalse(result.pending());
        assertNotNull(order.getNgayThanhToan());
    }

    @Test
    void keepsApprovedReturnPendingByDefaultUntilIpnArrives() {
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(11)
                .hoaDon(order)
                .maGiaoDich("VP42T3")
                .soTien(new BigDecimal("250000"))
                .trangThai("Chá» thanh toÃ¡n")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("VP42T3"))
                .thenReturn(Optional.of(payment));

        ShopPaymentGatewayService.GatewayReturnResult result = service.evaluateVnPayReturn(
                approvedReturn("VP42T3", "67891")
        );

        assertTrue(result.valid());
        assertFalse(result.success());
        assertTrue(result.pending());
    }

    @Test
    void acceptsValidZaloPayCallbackAndMarksOrderPaid() throws Exception {
        enableZaloPay();
        HoaDon order = payableOrder();
        ThanhToan payment = ThanhToan.builder()
                .id(21)
                .hoaDon(order)
                .maGiaoDich("260716_ZP42T1")
                .soTien(new BigDecimal("250000"))
                .trangThai("PENDING")
                .build();
        when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("260716_ZP42T1"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> callbackData = new LinkedHashMap<>();
        callbackData.put("app_id", TEST_ZALOPAY_APP_ID);
        callbackData.put("app_trans_id", "260716_ZP42T1");
        callbackData.put("amount", 250000);
        callbackData.put("zp_trans_id", 998877);
        String data = new ObjectMapper().writeValueAsString(callbackData);

        Map<String, Object> result = service.handleZaloPayCallback(Map.of(
                "data", data,
                "mac", hmacSha256(TEST_ZALOPAY_KEY2, data),
                "type", 1
        ));

        assertEquals(1L, ((Number) result.get("return_code")).longValue());
        assertNotNull(order.getNgayThanhToan());
    }

    @Test
    void reconcilesZaloPayReturnWhenCallbackWasMissed() throws Exception {
        enableZaloPay();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/query", exchange -> {
            byte[] response = """
                    {"return_code":1,"return_message":"Success","is_processing":false,
                     "amount":250000,"zp_trans_id":998877}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            properties.getZalopay().setQueryEndpoint(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v2/query"
            );
            HoaDon order = payableOrder();
            ThanhToan payment = ThanhToan.builder()
                    .id(22)
                    .hoaDon(order)
                    .maGiaoDich("260716_ZP42T2")
                    .soTien(new BigDecimal("250000"))
                    .trangThai("PENDING")
                    .build();
            when(paymentRepository.findFirstByMaGiaoDichOrderByIdDesc("260716_ZP42T2"))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(ThanhToan.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(any(HoaDon.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, String> params = approvedZaloPayReturn("260716_ZP42T2");
            ShopPaymentGatewayService.GatewayReturnResult result = service.evaluateZaloPayReturn(params);

            assertTrue(result.valid());
            assertTrue(result.success());
            assertFalse(result.pending());
            assertEquals("HD42", result.orderCode());
            assertNotNull(order.getNgayThanhToan());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsZaloPayReturnWithInvalidChecksum() {
        enableZaloPay();
        Map<String, String> params = approvedZaloPayReturn("260716_ZP42T3");
        params.put("checksum", "invalid");

        ShopPaymentGatewayService.GatewayReturnResult result = service.evaluateZaloPayReturn(params);

        assertFalse(result.valid());
        assertFalse(result.success());
        assertFalse(result.pending());
    }

    private HoaDon payableOrder() {
        return HoaDon.builder()
                .id(42)
                .maHoaDon("HD42")
                .tongTienThanhToan(new BigDecimal("250000"))
                .trangThai("Chờ thanh toán online")
                .ghiChu("Phương thức thanh toán: VNPAY")
                .daTruTon(true)
                .build();
    }

    private String sign(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        String query = sorted.entrySet().stream()
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        return hmacSha512(TEST_SECRET, query);
    }

    private Map<String, String> queryParams(String url) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = url.substring(url.indexOf('?') + 1);
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            params.put(
                    java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length > 1 ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : ""
            );
        }
        return params;
    }

    private Map<String, String> approvedReturn(String transactionRef, String transactionNo) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TmnCode", TEST_TMN_CODE);
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TransactionNo", transactionNo);
        params.put("vnp_SecureHash", sign(params));
        return params;
    }

    private void enableZaloPay() {
        properties.getZalopay().setEnabled(true);
        properties.getZalopay().setAppId(TEST_ZALOPAY_APP_ID);
        properties.getZalopay().setKey1(TEST_ZALOPAY_KEY1);
        properties.getZalopay().setKey2(TEST_ZALOPAY_KEY2);
        properties.getZalopay().setEndpoint("https://sb-openapi.zalopay.vn/v2/create");
        properties.getZalopay().setQueryEndpoint("https://sb-openapi.zalopay.vn/v2/query");
    }

    private Map<String, String> approvedZaloPayReturn(String transactionRef) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("orderCode", "HD42");
        params.put("appid", String.valueOf(TEST_ZALOPAY_APP_ID));
        params.put("apptransid", transactionRef);
        params.put("pmcid", "38");
        params.put("bankcode", "");
        params.put("amount", "250000");
        params.put("discountamount", "0");
        params.put("status", "1");
        String macInput = params.get("appid") + "|"
                + params.get("apptransid") + "|"
                + params.get("pmcid") + "|"
                + params.get("bankcode") + "|"
                + params.get("amount") + "|"
                + params.get("discountamount") + "|"
                + params.get("status");
        params.put("checksum", hmacSha256(TEST_ZALOPAY_KEY2, macInput));
        return params;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(result.length * 2);
            for (byte value : result) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(result.length * 2);
            for (byte value : result) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
