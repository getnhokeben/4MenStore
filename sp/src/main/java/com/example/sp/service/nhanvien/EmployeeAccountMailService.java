
package com.example.sp.service.nhanvien;

import com.example.sp.model.nhanvien.NhanVien;
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
public class EmployeeAccountMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.employee-login-url:http://localhost:8080/dang-nhap}")
    private String employeeLoginUrl;

    public void sendInitialAccount(
            NhanVien employee,
            String temporaryPassword
    ) {
        sendMail(
                employee.getEmail(),
                "Tài khoản nhân viên 4MenStore",
                """
                Chào %s,

                Tài khoản nhân viên 4MenStore của bạn đã được tạo.

                Email đăng nhập: %s
                Mật khẩu tạm thời: %s
                Đăng nhập tại: %s

                Vui lòng đổi mật khẩu ngay sau khi đăng nhập.

                Trân trọng,
                4MenStore
                """.formatted(
                        employee.getHoTen(),
                        employee.getEmail(),
                        temporaryPassword,
                        employeeLoginUrl
                )
        );
    }

    public void sendResetPassword(
            NhanVien employee,
            String temporaryPassword
    ) {
        sendMail(
                employee.getEmail(),
                "Khôi phục mật khẩu 4MenStore",
                """
                Chào %s,

                Hệ thống vừa nhận yêu cầu quên mật khẩu cho tài khoản của bạn.

                Email đăng nhập: %s
                Mật khẩu tạm thời mới: %s
                Đăng nhập tại: %s

                Sau khi đăng nhập, vui lòng đổi mật khẩu ngay.

                Trân trọng,
                4MenStore
                """.formatted(
                        employee.getHoTen(),
                        employee.getEmail(),
                        temporaryPassword,
                        employeeLoginUrl
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
                    "Thiếu spring.mail.username trong application.properties"
            );
        }

        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Nhân viên chưa có email để nhận mật khẩu"
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
                    "Gửi mail thất bại: " + root.getMessage(),
                    ex
            );
        }
    }
}
