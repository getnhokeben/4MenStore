package com.example.sp.service.cuahang;

import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.dto.cuahang.ShopOrderHistoryDTO;
import com.example.sp.dto.cuahang.ShopOrderHistoryItemDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShopOrderMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendOrderConfirmation(String toEmail, ShopOrderHistoryDTO order) {
        String email = safe(toEmail);
        if (email.isBlank() || fromEmail == null || fromEmail.isBlank()) {
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
            helper.setTo(email);
            helper.setSubject("4MenStore - Xac nhan don hang " + safe(order.getMaHoaDon()));
            helper.setText(mailBody(order), false);
            mailSender.send(message);
        } catch (MailException | MessagingException ex) {
            throw new IllegalStateException("Gui mail don hang that bai", ex);
        }
    }

    private String mailBody(ShopOrderHistoryDTO order) {
        StringBuilder body = new StringBuilder();
        body.append("Chao ").append(safe(order.getTenKhachHang())).append(",\n\n");
        body.append("4MenStore da tiep nhan don hang cua ban.\n\n");
        body.append("Ma don hang: ").append(safe(order.getMaHoaDon())).append('\n');
        body.append("Trang thai: ").append(safe(order.getTrangThai())).append('\n');
        body.append("Thoi gian dat: ").append(order.getNgayTao() == null ? "" : order.getNgayTao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append('\n');
        body.append("So dien thoai: ").append(safe(order.getSoDienThoai())).append('\n');
        body.append("Dia chi giao hang: ").append(safe(order.getDiaChiKhachHang())).append("\n\n");
        body.append("San pham:\n");
        for (ShopOrderHistoryItemDTO item : order.getItems()) {
            body.append("- ")
                    .append(safe(item.getTenSanPham()))
                    .append(" | ")
                    .append(safe(item.getMauSac()))
                    .append(" / ")
                    .append(safe(item.getKichCo()))
                    .append(" | SL: ")
                    .append(item.getSoLuong() == null ? 0 : item.getSoLuong())
                    .append(" | Thanh tien: ")
                    .append(formatMoney(item.getThanhTien()))
                    .append('\n');
        }
        body.append('\n');
        body.append("Tam tinh: ").append(formatMoney(order.getTongTienGoc())).append('\n');
        body.append("Giam gia: ").append(formatMoney(order.getSoTienGiam())).append('\n');
        body.append("Phi van chuyen: ").append(formatMoney(order.getPhiVanChuyen())).append('\n');
        body.append("Tong thanh toan: ").append(formatMoney(order.getTongTienThanhToan())).append("\n\n");
        body.append("Ban co the tra cuu don hang bang ma don hang.\n\n");
        body.append("Tran trong,\n4MenStore");
        return body.toString();
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safeValue = MoneyRoundingUtil.roundNonNegative(value);
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(safeValue) + " d";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
