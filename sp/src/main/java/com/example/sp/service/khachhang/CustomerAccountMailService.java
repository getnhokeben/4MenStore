package com.example.sp.service.khachhang;

import com.example.sp.model.khachhang.KhachHang;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class CustomerAccountMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.customer-login-url:http://localhost:8080/dang-nhap}")
    private String customerLoginUrl;

    public void sendInitialAccount(
            KhachHang customer,
            String temporaryPassword
    ) {
        sendMail(
                customer.getEmail(),
                "Tai khoan khach hang 4MenStore",
                """
                Chao %s,

                Tai khoan khach hang 4MenStore cua ban da duoc tao.

                Email dang nhap: %s
                Mat khau tam thoi: %s
                Dang nhap tai: %s

                Sau khi dang nhap, vui long doi mat khau de dam bao an toan.

                Tran trong,
                4MenStore
                """.formatted(
                        safe(customer.getTenKhachHang()),
                        safe(customer.getEmail()),
                        temporaryPassword,
                        customerLoginUrl
                )
        );
    }

    public void sendResetPassword(
            KhachHang customer,
            String temporaryPassword
    ) {
        sendMail(
                customer.getEmail(),
                "Khoi phuc mat khau khach hang 4MenStore",
                """
                Chao %s,

                He thong vua tao mat khau tam thoi moi cho tai khoan khach hang cua ban.

                Email dang nhap: %s
                Mat khau tam thoi moi: %s
                Dang nhap tai: %s

                Sau khi dang nhap, vui long doi mat khau ngay.

                Tran trong,
                4MenStore
                """.formatted(
                        safe(customer.getTenKhachHang()),
                        safe(customer.getEmail()),
                        temporaryPassword,
                        customerLoginUrl
                )
        );
    }

    private void sendMail(
            String toEmail,
            String subject,
            String content
    ) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException(
                    "Thieu spring.mail.username trong application.properties"
            );
        }

        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Khach hang chua co email de nhan mat khau"
            );
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
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(content, false);

            mailSender.send(message);
        } catch (MailException | MessagingException ex) {
            Throwable root = ex;

            while (root.getCause() != null) {
                root = root.getCause();
            }

            throw new IllegalStateException(
                    "Gui mail that bai: " + root.getMessage(),
                    ex
            );
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
