package com.example.sp.service.cuahang;

import com.example.sp.dto.cuahang.ShopOrderHistoryDTO;
import com.example.sp.dto.cuahang.ShopOrderHistoryItemDTO;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.service.tienich.TransactionalEmailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShopOrderMailService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.sender-name:4MenStore}")
    private String fromName;

    // Xử lý tương tác người dùng cho send order confirmation.
    public void sendOrderConfirmation(String toEmail, ShopOrderHistoryDTO order) {
        String email = safe(toEmail);
        if (email.isBlank() || fromEmail == null || fromEmail.isBlank()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail, senderName());
            helper.setReplyTo(fromEmail);
            helper.setTo(email);
            helper.setSubject("Đã nhận đơn hàng " + safe(order.getMaHoaDon()) + " | 4MenStore");
            TransactionalEmailTemplate.MailContent content = mailContent(order);
            helper.setText(content.text(), content.html());
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            throw new IllegalStateException("Gửi mail đơn hàng thất bại", ex);
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm mail content.
    private TransactionalEmailTemplate.MailContent mailContent(ShopOrderHistoryDTO order) {
        List<TransactionalEmailTemplate.LineItem> items = new ArrayList<>();
        if (order.getItems() != null) {
            for (ShopOrderHistoryItemDTO item : order.getItems()) {
                if (item == null) {
                    continue;
                }
                items.add(new TransactionalEmailTemplate.LineItem(
                        fallback(item.getTenSanPham(), "Sản phẩm"),
                        productVariant(item),
                        "Số lượng: " + (item.getSoLuong() == null ? 0 : item.getSoLuong()),
                        formatMoney(item.getThanhTien())
                ));
            }
        }

        List<TransactionalEmailTemplate.Section> sections = new ArrayList<>();
        sections.add(new TransactionalEmailTemplate.Section(
                "Thông tin đơn hàng",
                List.of(
                        new TransactionalEmailTemplate.Detail("Mã đơn hàng", safe(order.getMaHoaDon()), true),
                        new TransactionalEmailTemplate.Detail("Trạng thái", fallback(order.getTrangThai(), "Đã tiếp nhận"), true),
                        new TransactionalEmailTemplate.Detail("Thời gian đặt", order.getNgayTao() == null ? "" : order.getNgayTao().format(DATE_TIME_FORMAT))
                )
        ));
        sections.add(new TransactionalEmailTemplate.Section(
                "Thông tin giao hàng",
                List.of(
                        new TransactionalEmailTemplate.Detail("Số điện thoại", safe(order.getSoDienThoai())),
                        new TransactionalEmailTemplate.Detail("Địa chỉ nhận hàng", safe(order.getDiaChiKhachHang()))
                )
        ));
        sections.add(new TransactionalEmailTemplate.Section(
                "Thanh toán",
                List.of(
                        new TransactionalEmailTemplate.Detail("Tạm tính", formatMoney(order.getTongTienGoc())),
                        new TransactionalEmailTemplate.Detail("Giảm giá", formatMoney(order.getSoTienGiam())),
                        new TransactionalEmailTemplate.Detail("Phí vận chuyển", formatMoney(order.getPhiVanChuyen())),
                        new TransactionalEmailTemplate.Detail("Tổng thanh toán", formatMoney(order.getTongTienThanhToan()), true)
                )
        ));

        String customerName = fallback(order.getTenKhachHang(), "bạn");
        return TransactionalEmailTemplate.compose(
                "4MenStore đã nhận đơn hàng " + safe(order.getMaHoaDon()) + " của bạn.",
                "Xác nhận đơn hàng",
                "Chúng tôi đã nhận đơn hàng của bạn",
                "Chào " + customerName + ",",
                "Cảm ơn bạn đã chọn 4MenStore. Chúng tôi sẽ cập nhật qua email khi đơn hàng có thay đổi.",
                sections,
                items,
                null,
                null,
                "Hãy kiểm tra lại thông tin giao hàng. Nếu cần điều chỉnh, vui lòng phản hồi email này sớm nhất có thể."
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm product variant.
    private String productVariant(ShopOrderHistoryItemDTO item) {
        List<String> parts = new ArrayList<>();
        if (!safe(item.getMauSac()).isBlank()) {
            parts.add(safe(item.getMauSac()));
        }
        if (!safe(item.getKichCo()).isBlank()) {
            parts.add("Size " + safe(item.getKichCo()));
        }
        return parts.isEmpty() ? "" : String.join(" · ", parts);
    }

    // Thực hiện xử lý nghiệp vụ của hàm format money.
    private String formatMoney(BigDecimal value) {
        BigDecimal safeValue = MoneyRoundingUtil.roundNonNegative(value);
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(safeValue) + " ₫";
    }

    // Thực hiện xử lý nghiệp vụ của hàm fallback.
    private String fallback(String value, String fallback) {
        return safe(value).isBlank() ? fallback : safe(value);
    }

    // Thực hiện xử lý nghiệp vụ của hàm safe.
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    // Xử lý tương tác người dùng cho sender name.
    private String senderName() {
        return safe(fromName).isBlank() ? "4MenStore" : safe(fromName);
    }
}
