package com.example.sp.service.hoadon;

import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.service.tienich.TransactionalEmailTemplate;
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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    @Value("${app.mail.sender-name:4MenStore}")
    private String fromName;

    @Async
    // Xử lý tương tác người dùng cho send status changed.
    public void sendStatusChanged(HoaDon order, String oldStatus, String newStatus, String toEmail) {
        if (order == null || isBlank(toEmail) || isBlank(fromEmail)) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail, senderName());
            helper.setReplyTo(fromEmail);
            helper.setTo(toEmail.trim());
            helper.setSubject("Cập nhật đơn hàng " + safe(order.getMaHoaDon()) + " | 4MenStore");
            TransactionalEmailTemplate.MailContent content = mailContent(order, oldStatus, newStatus);
            helper.setText(content.text(), content.html());
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.warn("Không gửi được email cập nhật trạng thái đơn hàng {}", order.getMaHoaDon(), ex);
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm resolve customer email.
    public String resolveCustomerEmail(HoaDon order) {
        if (order == null) {
            return "";
        }
        if (order.getKhachHang() != null && !isBlank(order.getKhachHang().getEmail())) {
            return order.getKhachHang().getEmail().trim();
        }
        return readMarkedEmail(order.getGhiChu());
    }

    // Thực hiện xử lý nghiệp vụ của hàm append customer email marker.
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

    // Thực hiện xử lý nghiệp vụ của hàm strip customer email marker.
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

    // Tải hoặc truy xuất dữ liệu cho read marked email.
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

    // Thực hiện xử lý nghiệp vụ của hàm mail content.
    private TransactionalEmailTemplate.MailContent mailContent(HoaDon order, String oldStatus, String newStatus) {
        String customerName = isBlank(order.getTenKhachHang()) ? "bạn" : order.getTenKhachHang().trim();
        List<TransactionalEmailTemplate.Detail> deliveryDetails = new java.util.ArrayList<>();
        if (!isBlank(order.getDiaChiKhachHang())) {
            deliveryDetails.add(new TransactionalEmailTemplate.Detail("Địa chỉ nhận hàng", order.getDiaChiKhachHang()));
        }
        if (!isBlank(order.getSoDienThoai())) {
            deliveryDetails.add(new TransactionalEmailTemplate.Detail("Số điện thoại", order.getSoDienThoai()));
        }

        List<TransactionalEmailTemplate.Section> sections = new java.util.ArrayList<>();
        sections.add(new TransactionalEmailTemplate.Section(
                "Tình trạng đơn hàng",
                List.of(
                        new TransactionalEmailTemplate.Detail("Mã đơn hàng", safe(order.getMaHoaDon()), true),
                        new TransactionalEmailTemplate.Detail("Trạng thái trước", displayStatus(oldStatus)),
                        new TransactionalEmailTemplate.Detail("Trạng thái hiện tại", displayStatus(newStatus), true),
                        new TransactionalEmailTemplate.Detail("Cập nhật lúc", formatDateTime(order.getNgayCapNhat())),
                        new TransactionalEmailTemplate.Detail("Tổng thanh toán", formatMoney(order.getTongTienThanhToan()), true)
                )
        ));
        if (!deliveryDetails.isEmpty()) {
            sections.add(new TransactionalEmailTemplate.Section("Thông tin giao nhận", deliveryDetails));
        }

        return TransactionalEmailTemplate.compose(
                "Đơn hàng " + safe(order.getMaHoaDon()) + " vừa được cập nhật.",
                "Cập nhật đơn hàng",
                "Đơn hàng của bạn có thay đổi mới",
                "Chào " + customerName + ",",
                statusMessage(newStatus),
                sections,
                List.of(),
                null,
                null,
                "Thông tin này được gửi vì trạng thái đơn hàng của bạn vừa thay đổi."
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm status message.
    private String statusMessage(String status) {
        String normalized = normalize(status);
        if (normalized.contains("xac nhan")) {
            return "Đơn hàng của bạn đã được xác nhận và sẽ sớm được xử lý.";
        }
        if (normalized.contains("chuan bi")) {
            return "Đơn hàng đang được chuẩn bị. Chúng tôi sẽ tiếp tục cập nhật khi có thay đổi.";
        }
        if (normalized.contains("cho hang hoan") || normalized.contains("cho hoan hang")) {
            return "Đơn hàng đang được vận chuyển hoàn về kho. Sản phẩm sẽ được kiểm tra khi cửa hàng nhận lại.";
        }
        if (normalized.contains("cho nhap hang") || normalized.contains("cho hang ve")) {
            return "Một hoặc nhiều sản phẩm đang chờ nhập thêm. Cửa hàng sẽ xác nhận lại đơn ngay khi có hàng.";
        }
        if (normalized.contains("giao")) {
            return "Đơn hàng đang trên đường giao đến bạn.";
        }
        if (normalized.contains("hoan") || normalized.contains("thanh toan")) {
            return "Đơn hàng đã hoàn tất. Cảm ơn bạn đã mua sắm tại 4MenStore.";
        }
        if (normalized.contains("huy")) {
            return "Đơn hàng đã được hủy. Nếu cần hỗ trợ thêm, vui lòng phản hồi email này.";
        }
        return "Bạn có thể lưu mã đơn hàng để tiện tra cứu và theo dõi.";
    }

    // Hiển thị và đồng bộ giao diện cho display status.
    private String displayStatus(String status) {
        return isBlank(status) ? "Chưa cập nhật" : status.trim();
    }

    // Thực hiện xử lý nghiệp vụ của hàm format date time.
    private String formatDateTime(LocalDateTime value) {
        return (value == null ? LocalDateTime.now() : value).format(DATE_TIME_FORMAT);
    }

    // Thực hiện xử lý nghiệp vụ của hàm format money.
    private String formatMoney(BigDecimal value) {
        BigDecimal safeValue = MoneyRoundingUtil.roundNonNegative(value);
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(safeValue) + " ₫";
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize.
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

    // Thực hiện xử lý nghiệp vụ của hàm safe.
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    // Xử lý tương tác người dùng cho sender name.
    private String senderName() {
        return safe(fromName).isBlank() ? "4MenStore" : safe(fromName);
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is blank.
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
