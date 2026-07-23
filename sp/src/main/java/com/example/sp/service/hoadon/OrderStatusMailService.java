package com.example.sp.service.hoadon;

import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.model.hoadon.HoaDon;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusMailService {

    public static final String CUSTOMER_EMAIL_MARKER = "CUSTOMER_EMAIL:";

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Async
    public void sendStatusChanged(HoaDon order, String oldStatus, String newStatus, String toEmail) {
        if (order == null || isBlank(toEmail) || isBlank(fromEmail)) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(fromEmail);
            helper.setReplyTo(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("4MenStore - Cập nhật đơn hàng " + safe(order.getMaHoaDon()));
            helper.setText(mailBody(order, oldStatus, newStatus), false);
            mailSender.send(message);
        } catch (MailException | MessagingException ex) {
            log.warn("Không gửi được mail cập nhật trạng thái đơn hàng {}", order.getMaHoaDon(), ex);
        }
    }

    public String resolveCustomerEmail(HoaDon order) {
        if (order == null) {
            return "";
        }
        if (order.getKhachHang() != null && !isBlank(order.getKhachHang().getEmail())) {
            return order.getKhachHang().getEmail().trim();
        }
        return readMarkedEmail(order.getGhiChu());
    }

    public static String appendCustomerEmailMarker(String note, String email) {
        String cleanedNote = stripCustomerEmailMarker(note);
        if (isBlank(email)) {
            return cleanedNote;
        }
        String markerLine = CUSTOMER_EMAIL_MARKER + email.trim().toLowerCase(Locale.ROOT);
        if (isBlank(cleanedNote)) {
            return markerLine;
        }
        return cleanedNote + "\n" + markerLine;
    }

    public static String stripCustomerEmailMarker(String note) {
        if (note == null || note.isBlank()) {
            return note;
        }
        StringBuilder visible = new StringBuilder();
        for (String line : note.split("\\R")) {
            if (line.trim().startsWith(CUSTOMER_EMAIL_MARKER)) {
                continue;
            }
            if (visible.length() > 0) {
                visible.append('\n');
            }
            visible.append(line);
        }
        String result = visible.toString().trim();
        return result.isBlank() ? null : result;
    }

    private String readMarkedEmail(String note) {
        if (note == null || note.isBlank()) {
            return "";
        }
        for (String line : note.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(CUSTOMER_EMAIL_MARKER)) {
                return trimmed.substring(CUSTOMER_EMAIL_MARKER.length()).trim();
            }
        }
        return "";
    }

    private String mailBody(HoaDon order, String oldStatus, String newStatus) {
        String customerName = isBlank(order.getTenKhachHang()) ? "quý khách" : order.getTenKhachHang().trim();
        StringBuilder body = new StringBuilder();
        body.append("Chào ").append(customerName).append(",\n\n");
        body.append("4MenStore vừa cập nhật trạng thái đơn hàng của bạn.\n\n");
        body.append("Mã đơn hàng: ").append(safe(order.getMaHoaDon())).append('\n');
        body.append("Trạng thái cũ: ").append(displayStatus(oldStatus)).append('\n');
        body.append("Trạng thái mới: ").append(displayStatus(newStatus)).append('\n');
        body.append("Thời gian cập nhật: ").append(formatDateTime(order.getNgayCapNhat())).append('\n');
        body.append("Tổng thanh toán: ").append(formatMoney(order.getTongTienThanhToan())).append('\n');
        if (!isBlank(order.getDiaChiKhachHang())) {
            body.append("Địa chỉ nhận hàng: ").append(order.getDiaChiKhachHang().trim()).append('\n');
        }
        if (!isBlank(order.getSoDienThoai())) {
            body.append("Số điện thoại: ").append(order.getSoDienThoai().trim()).append('\n');
        }
        body.append('\n').append(statusMessage(newStatus)).append("\n\n");
        body.append("Cảm ơn bạn đã mua sắm tại 4MenStore.\n\n");
        body.append("Trân trọng,\n4MenStore");
        return body.toString();
    }

    private String statusMessage(String status) {
        String normalized = normalize(status);
        if (normalized.contains("xac nhan")) {
            return "Đơn hàng của bạn đã được xác nhận và sẽ sớm được xử lý.";
        }
        if (normalized.contains("chuan bi")) {
            return "Đơn hàng đang được chuẩn bị để bạn có thể theo dõi tiến trình.";
        }
        if (normalized.contains("cho hang hoan") || normalized.contains("cho hoan hang")) {
            return "Đơn hàng đang được vận chuyển hoàn về kho. Sản phẩm chưa được nhập lại tồn kho cho đến khi cửa hàng nhận và kiểm tra hàng.";
        }
        if (normalized.contains("giao")) {
            return "Đơn hàng đang trên đường giao đến bạn.";
        }
        if (normalized.contains("hoan") || normalized.contains("thanh toan")) {
            return "Đơn hàng đã hoàn tất. Hẹn gặp lại bạn trong những đơn hàng tiếp theo.";
        }
        if (normalized.contains("huy")) {
            return "Đơn hàng đã được hủy. Nếu cần hỗ trợ thêm, vui lòng liên hệ 4MenStore.";
        }
        return "Bạn có thể dùng mã đơn hàng để tra cứu và theo dõi đơn hàng.";
    }

    private String displayStatus(String status) {
        return isBlank(status) ? "Chưa cập nhật" : status.trim();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? LocalDateTime.now().format(DATE_TIME_FORMAT) : value.format(DATE_TIME_FORMAT);
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safeValue = MoneyRoundingUtil.roundNonNegative(value);
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(safeValue) + " đ";
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
