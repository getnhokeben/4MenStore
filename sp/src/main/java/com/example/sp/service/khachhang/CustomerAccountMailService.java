package com.example.sp.service.khachhang;

import com.example.sp.model.khachhang.KhachHang;
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
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAccountMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.sender-name:4MenStore}")
    private String fromName;

    @Value("${app.customer-login-url:http://localhost:8080/dang-nhap}")
    private String customerLoginUrl;

    // Xử lý tương tác người dùng cho send initial account.
    public void sendInitialAccount(KhachHang customer, String temporaryPassword) {
        sendMail(
                customer.getEmail(),
                "Tài khoản 4MenStore của bạn đã sẵn sàng",
                TransactionalEmailTemplate.compose(
                        "Thông tin đăng nhập 4MenStore của bạn.",
                        "Tài khoản khách hàng",
                        "Chào mừng bạn đến với 4MenStore",
                        "Chào " + displayName(customer.getTenKhachHang()) + ",",
                        "Tài khoản khách hàng của bạn đã được tạo. Dùng thông tin bên dưới để đăng nhập và bắt đầu mua sắm.",
                        List.of(new TransactionalEmailTemplate.Section(
                                "Thông tin đăng nhập",
                                List.of(
                                        new TransactionalEmailTemplate.Detail("Email đăng nhập", safe(customer.getEmail())),
                                        new TransactionalEmailTemplate.Detail("Mật khẩu tạm thời", safe(temporaryPassword), true)
                                )
                        )),
                        List.of(),
                        "Đăng nhập tài khoản",
                        customerLoginUrl,
                        "Để bảo vệ tài khoản, hãy đổi mật khẩu ngay sau lần đăng nhập đầu tiên."
                )
        );
    }

    // Xử lý tương tác người dùng cho send reset password.
    public void sendResetPassword(KhachHang customer, String temporaryPassword) {
        sendMail(
                customer.getEmail(),
                "Mật khẩu mới cho tài khoản 4MenStore",
                TransactionalEmailTemplate.compose(
                        "Yêu cầu đặt lại mật khẩu tài khoản 4MenStore.",
                        "Bảo mật tài khoản",
                        "Mật khẩu tạm thời của bạn",
                        "Chào " + displayName(customer.getTenKhachHang()) + ",",
                        "Chúng tôi đã tạo mật khẩu tạm thời mới theo yêu cầu đặt lại mật khẩu của bạn.",
                        List.of(new TransactionalEmailTemplate.Section(
                                "Thông tin đăng nhập",
                                List.of(
                                        new TransactionalEmailTemplate.Detail("Email đăng nhập", safe(customer.getEmail())),
                                        new TransactionalEmailTemplate.Detail("Mật khẩu tạm thời mới", safe(temporaryPassword), true)
                                )
                        )),
                        List.of(),
                        "Đăng nhập và đổi mật khẩu",
                        customerLoginUrl,
                        "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy phản hồi email này để được hỗ trợ."
                )
        );
    }

    // Xử lý tương tác người dùng cho send mail.
    private void sendMail(String toEmail, String subject, TransactionalEmailTemplate.MailContent content) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("Thiếu spring.mail.username trong application.properties");
        }
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Khách hàng chưa có email để nhận mật khẩu");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail, senderName());
            helper.setReplyTo(fromEmail);
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);
            helper.setText(content.text(), content.html());
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            throw new IllegalStateException("Gửi mail thất bại: " + rootMessage(ex), ex);
        }
    }

    // Hiển thị và đồng bộ giao diện cho display name.
    private String displayName(String name) {
        return safe(name).isBlank() ? "bạn" : safe(name);
    }

    // Xử lý tương tác người dùng cho sender name.
    private String senderName() {
        return safe(fromName).isBlank() ? "4MenStore" : safe(fromName);
    }

    // Thực hiện xử lý nghiệp vụ của hàm safe.
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    // Thực hiện xử lý nghiệp vụ của hàm root message.
    private String rootMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
