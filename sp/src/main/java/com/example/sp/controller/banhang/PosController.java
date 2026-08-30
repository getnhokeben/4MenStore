package com.example.sp.controller.banhang;

import com.example.sp.dto.banhang.PosCheckoutRequest;
import com.example.sp.dto.banhang.PosCustomerRequest;
import com.example.sp.dto.banhang.PosOrderDTO;
import com.example.sp.dto.banhang.PosOrderItemDTO;
import com.example.sp.dto.banhang.PosOrderItemRequest;
import com.example.sp.dto.banhang.PosShippingRequest;
import com.example.sp.dto.banhang.PosVoucherDTO;
import com.example.sp.dto.banhang.PosVoucherRequest;
import com.example.sp.dto.cuahang.ShopPaymentResponse;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import com.example.sp.service.banhang.PosService;
import com.example.sp.service.cuahang.ShopPaymentGatewayService;
import com.example.sp.service.giaohang.GhnShippingService;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;
    private final GhnShippingService ghnShippingService;
    private final ShopPaymentGatewayService paymentGatewayService;

    @GetMapping(value = {"/ban-hang-tai-quay", "/ban-hang-tai-quay.html", "/ban-hang", "/admin/pos"}, produces = MediaType.TEXT_HTML_VALUE)
    // Thực hiện xử lý nghiệp vụ của hàm page.
    public ResponseEntity<byte[]> page() throws IOException {
        byte[] html = new ClassPathResource("templates/ban-hang-tai-quay.html").getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    @PostMapping("/api/pos/orders")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create order.
    public PosOrderDTO createOrder(HttpSession session) {
        Integer employeeId = (Integer) session.getAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
        return posService.createOrder(employeeId);
    }

    @GetMapping("/api/pos/orders")
    // Thực hiện xử lý nghiệp vụ của hàm pending orders.
    public List<PosOrderDTO> pendingOrders() {
        return posService.pendingOrders();
    }

    @GetMapping("/api/pos/orders/{id}")
    // Tải hoặc truy xuất dữ liệu cho get order.
    public PosOrderDTO getOrder(@PathVariable Integer id) {
        return posService.getOrder(id);
    }

    @PostMapping("/api/pos/orders/{id}/items")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho add item.
    public PosOrderDTO addItem(@PathVariable Integer id, @Valid @RequestBody PosOrderItemRequest request) {
        return posService.addItem(id, request);
    }

    @PatchMapping("/api/pos/orders/{id}/items/{idHdct}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update item.
    public PosOrderDTO updateItem(@PathVariable Integer id,
                                  @PathVariable Integer idHdct,
                                  @RequestBody Map<String, Integer> body) {
        return posService.updateItem(id, idHdct, body.get("soLuong"));
    }

    @DeleteMapping("/api/pos/orders/{id}/items/{idHdct}")
    // Xử lý thao tác đóng, xóa hoặc hủy cho remove item.
    public PosOrderDTO removeItem(@PathVariable Integer id, @PathVariable Integer idHdct) {
        return posService.removeItem(id, idHdct);
    }

    @PutMapping("/api/pos/orders/{id}/customer")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho set customer.
    public PosOrderDTO setCustomer(@PathVariable Integer id, @RequestBody PosCustomerRequest request) {
        return posService.setCustomer(id, request);
    }

    @PutMapping("/api/pos/orders/{id}/shipping")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho set shipping.
    public PosOrderDTO setShipping(@PathVariable Integer id, @RequestBody PosShippingRequest request) {
        return posService.setShipping(id, request);
    }

    @GetMapping("/api/pos/shipping/provinces")
    // Thực hiện xử lý nghiệp vụ của hàm provinces.
    public ResponseEntity<String> provinces() {
        return json(ghnShippingService.provinces());
    }

    @GetMapping("/api/pos/shipping/districts")
    // Thực hiện xử lý nghiệp vụ của hàm districts.
    public ResponseEntity<String> districts(@RequestParam Integer provinceId) {
        return json(ghnShippingService.districts(provinceId));
    }

    @GetMapping("/api/pos/shipping/wards")
    // Thực hiện xử lý nghiệp vụ của hàm wards.
    public ResponseEntity<String> wards(@RequestParam Integer districtId) {
        return json(ghnShippingService.wards(districtId));
    }

    @PostMapping("/api/pos/shipping/fee")
    // Thực hiện xử lý nghiệp vụ của hàm shipping fee.
    public Map<String, BigDecimal> shippingFee(@RequestBody PosShippingRequest request) {
        BigDecimal fee = request.getDistrictId() != null && request.getDistrictId() > 0
                ? ghnShippingService.calculateFeeWithFallback(
                        request.getDistrictId(),
                        request.getWardCode(),
                        request.getPhiVanChuyen(),
                        request.getProvinceName(),
                        request.getWardName())
                : ghnShippingService.calculateFallbackFee(
                        request.getProvinceName(),
                        request.getWardName(),
                        request.getPhiVanChuyen());
        return Map.of("fee", MoneyRoundingUtil.roundNonNegative(fee));
    }

    @PostMapping("/api/pos/orders/{id}/voucher")
    // Thực hiện xử lý nghiệp vụ của hàm apply voucher.
    public PosOrderDTO applyVoucher(@PathVariable Integer id, @Valid @RequestBody PosVoucherRequest request) {
        return posService.applyVoucher(id, request.getMaVoucher());
    }

    @GetMapping("/api/pos/orders/{id}/vouchers")
    // Thực hiện xử lý nghiệp vụ của hàm vouchers.
    public List<PosVoucherDTO> vouchers(@PathVariable Integer id) {
        return posService.availableVouchers(id);
    }

    @DeleteMapping("/api/pos/orders/{id}/voucher")
    // Xử lý thao tác đóng, xóa hoặc hủy cho remove voucher.
    public PosOrderDTO removeVoucher(@PathVariable Integer id) {
        return posService.removeVoucher(id);
    }

    @PostMapping("/api/pos/orders/{id}/checkout")
    // Kiểm tra điều kiện và tính hợp lệ cho checkout.
    public PosOrderDTO checkout(@PathVariable Integer id,
                                @Valid @RequestBody PosCheckoutRequest request,
                                HttpSession session) {
        Integer employeeId = (Integer) session.getAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
        return posService.checkout(id, request, employeeId);
    }

    @PostMapping("/api/pos/orders/{id}/vnpay")
    // Thực hiện xử lý nghiệp vụ của hàm start vn pay payment.
    public ShopPaymentResponse startVnPayPayment(@PathVariable Integer id,
                                                  HttpSession session,
                                                  HttpServletRequest request) {
        if (!Boolean.TRUE.equals(paymentGatewayService.availability().get("VNPAY"))) {
            throw new IllegalStateException("Cổng VNPay chưa được cấu hình đầy đủ");
        }
        Integer employeeId = (Integer) session.getAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
        PosOrderDTO order = posService.prepareVnPayCheckout(id, employeeId);
        return paymentGatewayService.createPayment(
                order.getId(),
                order.getMaHoaDon(),
                "VNPAY",
                clientIp(request)
        );
    }

    @PostMapping("/api/pos/orders/{id}/cancel")
    // Xử lý thao tác đóng, xóa hoặc hủy cho cancel.
    public PosOrderDTO cancel(@PathVariable Integer id) {
        return posService.cancel(id);
    }

    @GetMapping("/api/pos/variants/by-code")
    // Tải hoặc truy xuất dữ liệu cho find variant by code.
    public PosOrderItemDTO findVariantByCode(@RequestParam String code) {
        return posService.findVariantByCode(code);
    }

    // Thực hiện xử lý nghiệp vụ của hàm client ip.
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Thực hiện xử lý nghiệp vụ của hàm json.
    private ResponseEntity<String> json(String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
