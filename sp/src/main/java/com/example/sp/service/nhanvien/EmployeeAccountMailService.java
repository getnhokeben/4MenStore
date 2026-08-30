package com.example.sp.service.nhanvien;

import com.example.sp.model.nhanvien.NhanVien;
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
public class EmployeeAccountMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.sender-name:4MenStore}")
    private String fromName;

    @Value("${app.employee-login-url:http://localhost:8080/dang-nhap}")
    private String employeeLoginUrl;

    // Xử lý tương tác người dùng cho send initial account.
    public void sendInitialAccount(NhanVien employee, String temporaryPassword) {
        sendMail(
                employee.getEmail(),
                "Tài khoản nhân viên 4MenStore đã sẵn sàng",
                TransactionalEmailTemplate.compose(
                        "Thông tin đăng nhập hệ thống 4MenStore của bạn.",
                        "Tài khoản nhân viên",
                        "Tài khoản của bạn đã được tạo",
                        "Chào " + displayName(employee.getHoTen()) + ",",
                        "Bạn đã được cấp quyền truy cập hệ thống 4MenStore. Vui lòng dùng thông tin bên dưới để đăng nhập.",
                        List.of(new TransactionalEmailTemplate.Section(
                                "Thông tin đăng nhập",
                                List.of(
                                        new TransactionalEmailTemplate.Detail("Email đăng nhập", safe(employee.getEmail())),
                                        new TransactionalEmailTemplate.Detail("Mật khẩu tạm thời", safe(temporaryPassword), true)
                                )
                        )),
                        List.of(),
                        "Đăng nhập hệ thống",
                        employeeLoginUrl,
                        "Vui lòng đổi mật khẩu ngay sau lần đăng nhập đầu tiên và không chia sẻ mật khẩu với người khác."
                )
        );
    }

    // Xử lý tương tác người dùng cho send reset password.
    public void sendResetPassword(NhanVien employee, String temporaryPassword) {
        sendMail(
                employee.getEmail(),
                "Mật khẩu mới cho tài khoản 4MenStore",
                TransactionalEmailTemplate.compose(
                        "Yêu cầu đặt lại mật khẩu hệ thống 4MenStore.",
                        "Bảo mật tài khoản",
                        "Mật khẩu tạm thời của bạn",
                        "Chào " + displayName(employee.getHoTen()) + ",",
                        "Chúng tôi đã tạo mật khẩu tạm thời mới theo yêu cầu đặt lại mật khẩu của bạn.",
                        List.of(new TransactionalEmailTemplate.Section(
                                "Thông tin đăng nhập",
                                List.of(
                                        new TransactionalEmailTemplate.Detail("Email đăng nhập", safe(employee.getEmail())),
                                        new TransactionalEmailTemplate.Detail("Mật khẩu tạm thời mới", safe(temporaryPassword), true)
                                )
                        )),
                        List.of(),
                        "Đăng nhập và đổi mật khẩu",
                        employeeLoginUrl,
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
            throw new IllegalArgumentException("Nhân viên chưa có email để nhận mật khẩu");
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
