
package com.example.sp.controller.khachhang;

import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.service.khachhang.KhachHangService;
import com.example.sp.service.cuahang.ShopSessionKeys;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {

    private final KhachHangService khachHangService;
    private final KhachHangRepository khachHangRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @GetMapping
    public Page<KhachHang> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) String gioiTinh,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return khachHangService.getAll(
                keyword,
                trangThai,
                gioiTinh,
                pageable
        );
    }

    @GetMapping("/{id:\\d+}")
    public KhachHang getById(@PathVariable Integer id) {
        return khachHangService.findById(id);
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody KhachHang request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(validationErrors(bindingResult));
        }

        KhachHang created = khachHangService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @Valid @RequestBody KhachHang request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(validationErrors(bindingResult));
        }

        return ResponseEntity.ok(
                khachHangService.update(id, request)
        );
    }

    @PatchMapping("/{id:\\d+}/trang-thai")
    public KhachHang toggleStatus(@PathVariable Integer id) {
        return khachHangService.toggleStatus(id);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        khachHangService.deactivate(id);

        return ResponseEntity.noContent().build();
    }

    public static class ChangePasswordRequest {

        @JsonAlias({
                "oldPassword",
                "currentPassword",
                "matKhauHienTai"
        })
        private String matKhauCu;

        @JsonAlias({"newPassword"})
        private String matKhauMoi;

        @JsonAlias({
                "confirmPassword",
                "confirmNewPassword"
        })
        private String xacNhanMatKhau;

        public String getMatKhauCu() {
            return matKhauCu;
        }

        public void setMatKhauCu(String matKhauCu) {
            this.matKhauCu = matKhauCu;
        }

        public String getMatKhauMoi() {
            return matKhauMoi;
        }

        public void setMatKhauMoi(String matKhauMoi) {
            this.matKhauMoi = matKhauMoi;
        }

        public String getXacNhanMatKhau() {
            return xacNhanMatKhau;
        }

        public void setXacNhanMatKhau(String xacNhanMatKhau) {
            this.xacNhanMatKhau = xacNhanMatKhau;
        }
    }

    @RequestMapping(
            value = {"/change-password", "/doi-mat-khau"},
            method = {RequestMethod.PUT, RequestMethod.POST}
    )
    @Transactional
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            HttpSession session
    ) {
        Integer customerId = (Integer) session.getAttribute(
                ShopSessionKeys.CUSTOMER_ID
        );

        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Bạn chưa đăng nhập")
            );
        }

        KhachHang customer = khachHangRepository
                .findById(customerId)
                .orElse(null);

        if (customer == null || !Boolean.TRUE.equals(customer.getTrangThai())) {
            session.removeAttribute(ShopSessionKeys.CUSTOMER_ID);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of(
                            "message",
                            "Tài khoản không tồn tại hoặc đã bị khóa"
                    )
            );
        }

        String oldPassword = request.getMatKhauCu() == null
                ? ""
                : request.getMatKhauCu();

        String newPassword = request.getMatKhauMoi() == null
                ? ""
                : request.getMatKhauMoi();

        String confirmPassword = request.getXacNhanMatKhau() == null
                ? ""
                : request.getXacNhanMatKhau();

        if (
                oldPassword.isBlank()
                        || newPassword.isBlank()
                        || confirmPassword.isBlank()
        ) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Vui lòng điền đầy đủ thông tin")
            );
        }

        if (newPassword.length() < 6 || newPassword.length() > 100) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Mật khẩu mới phải có từ 6 đến 100 ký tự"
                    )
            );
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Xác nhận mật khẩu mới không khớp")
            );
        }

        if (oldPassword.equals(newPassword)) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Mật khẩu mới không được trùng mật khẩu hiện tại"
                    )
            );
        }

        if (!matchesPassword(oldPassword, customer.getMatKhau())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Mật khẩu hiện tại không đúng")
            );
        }

        customer.setMatKhau(passwordEncoder.encode(newPassword));
        khachHangRepository.save(customer);

        return ResponseEntity.ok(
                Map.of("message", "Đổi mật khẩu thành công")
        );
    }

    @GetMapping("/xuat-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) String gioiTinh
    ) throws IOException {
        List<KhachHang> customers = khachHangService
                .getAll(keyword, trangThai, gioiTinh, Pageable.unpaged())
                .getContent();

        byte[] data = createExcel(customers);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=danh_sach_khach_hang.xlsx"
                )
                .body(data);
    }

    private boolean matchesPassword(
            String rawPassword,
            String storedPassword
    ) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        return isBcryptHash(storedPassword)
                ? passwordEncoder.matches(rawPassword, storedPassword)
                : storedPassword.equals(rawPassword);
    }

    private boolean isBcryptHash(String value) {
        return value != null && (
                value.startsWith("$2a$")
                        || value.startsWith("$2b$")
                        || value.startsWith("$2y$")
        );
    }

    private Map<String, Object> validationErrors(
            BindingResult bindingResult
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        return Map.of(
                "message", "Dữ liệu không hợp lệ",
                "errors", errors
        );
    }

    private byte[] createExcel(List<KhachHang> customers)
            throws IOException {

        String[] headers = {
                "STT",
                "Mã KH",
                "Tên khách hàng",
                "SĐT chính",
                "Email",
                "CCCD",
                "Địa chỉ mặc định",
                "Giới tính",
                "Ngày sinh",
                "Trạng thái"
        };

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Khách hàng");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);

            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex()
            );
            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dateFormat =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (int index = 0; index < customers.size(); index++) {
                KhachHang customer = customers.get(index);
                Row row = sheet.createRow(index + 1);

                row.createCell(0).setCellValue(index + 1);
                row.createCell(1).setCellValue(text(customer.getMaKh()));
                row.createCell(2).setCellValue(text(customer.getTenKhachHang()));
                row.createCell(3).setCellValue(text(customer.getSoDienThoai()));
                row.createCell(4).setCellValue(text(customer.getEmail()));
                row.createCell(5).setCellValue(text(customer.getCccd()));
                row.createCell(6).setCellValue(text(customer.getDiaChiDisplay()));
                row.createCell(7).setCellValue(text(customer.getGioiTinh()));
                row.createCell(8).setCellValue(
                        customer.getNgaySinh() == null
                                ? ""
                                : customer.getNgaySinh().format(dateFormat)
                );
                row.createCell(9).setCellValue(
                        Boolean.TRUE.equals(customer.getTrangThai())
                                ? "Hoạt động"
                                : "Ngừng hoạt động"
                );
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(output);

            return output.toByteArray();
        }
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
