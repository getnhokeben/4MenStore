package com.example.sp.controller.nhanvien;

import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.service.nhanvien.NhanVienService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienController {

    private final NhanVienService nhanVienService;
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;

    @GetMapping
    // Tải hoặc truy xuất dữ liệu cho get all.
    public Page<NhanVien> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String vaiTro,
            @RequestParam(required = false) Boolean trangThai,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return nhanVienService.getAll(
                keyword,
                vaiTro,
                trangThai,
                pageable
        );
    }

    @GetMapping("/tra-cuu-cccd/{cccd}")
    // Thực hiện xử lý nghiệp vụ của hàm lookup by cccd.
    public Map<String, Object> lookupByCccd(
            @PathVariable String cccd,
            @RequestParam(required = false) Integer editingEmployeeId
    ) {
        String normalizedCccd = cccd == null
                ? ""
                : cccd.replaceAll("\\D", "");

        if (!normalizedCccd.matches("^\\d{12}$")) {
            throw new IllegalArgumentException(
                    "CCCD phải gồm đúng 12 chữ số"
            );
        }

        return nhanVienRepository.findByCccd(normalizedCccd)
                .<Map<String, Object>>map(this::employeeLookup)
                .or(() -> khachHangRepository.findByCccd(normalizedCccd)
                        .map(this::customerLookup))
                .orElseGet(() -> Map.of(
                        "found", false,
                        "cccd", normalizedCccd
                ));
    }

    @GetMapping("/{id}")
    // Tải hoặc truy xuất dữ liệu cho get by id.
    public NhanVien getById(@PathVariable Integer id) {
        return nhanVienService.findById(id);
    }

    @PostMapping
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create.
    public ResponseEntity<NhanVien> create(
            @RequestBody NhanVien nhanVien
    ) {
        return ResponseEntity.status(201)
                .body(nhanVienService.create(nhanVien));
    }

    @PutMapping("/{id}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update.
    public NhanVien update(
            @PathVariable Integer id,
            @RequestBody NhanVien nhanVien
    ) {
        return nhanVienService.update(id, nhanVien);
    }

    @PatchMapping("/{id}/trang-thai")
    // Xử lý tương tác người dùng cho toggle status.
    public NhanVien toggleStatus(@PathVariable Integer id) {
        return nhanVienService.toggleStatus(id);
    }

    @DeleteMapping("/{id}")
    // Thực hiện xử lý nghiệp vụ của hàm deactivate.
    public ResponseEntity<Void> deactivate(
            @PathVariable Integer id
    ) {
        nhanVienService.deactivate(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/xuat-excel")
    // Thực hiện xử lý nghiệp vụ của hàm export excel.
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String vaiTro,
            @RequestParam(required = false) Boolean trangThai
    ) throws IOException {
        List<NhanVien> employees = nhanVienService
                .getAll(keyword, vaiTro, trangThai, Pageable.unpaged())
                .getContent();

        byte[] excel = createExcel(employees);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=danh_sach_nhan_vien.xlsx"
                )
                .contentLength(excel.length)
                .body(excel);
    }

    // Thực hiện xử lý nghiệp vụ của hàm employee lookup.
    private Map<String, Object> employeeLookup(
            NhanVien employee
    ) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("found", true);
        data.put("source", "NHAN_VIEN");
        data.put("employeeId", employee.getId());
        data.put("cccd", employee.getCccd());
        data.put("hoTen", employee.getHoTen());
        data.put("email", employee.getEmail());
        data.put("soDienThoai", employee.getSoDienThoai());
        data.put("ngaySinh", employee.getNgaySinh());
        data.put("gioiTinh", employee.getGioiTinh());
        data.put("diaChiChiTiet", employee.getDiaChiChiTiet());
        data.put("phuongXa", employee.getPhuongXa());
        data.put("quanHuyen", employee.getQuanHuyen());
        data.put("tinhThanh", employee.getTinhThanh());
        data.put("diaChiDisplay", employee.getDiaChiDisplay());

        return data;
    }

    // Thực hiện xử lý nghiệp vụ của hàm customer lookup.
    private Map<String, Object> customerLookup(
            KhachHang customer
    ) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("found", true);
        data.put("source", "KHACH_HANG");
        data.put("employeeId", null);
        data.put("cccd", customer.getCccd());
        data.put("hoTen", customer.getTenKhachHang());
        data.put("email", customer.getEmail());
        data.put("soDienThoai", customer.getSoDienThoai());
        data.put("ngaySinh", customer.getNgaySinh());
        data.put("gioiTinh", customer.getGioiTinh());
        data.put("diaChiChiTiet", customer.getDiaChiChiTiet());
        data.put("phuongXa", customer.getPhuongXa());
        data.put("quanHuyen", customer.getQuanHuyen());
        data.put("tinhThanh", customer.getTinhThanh());
        data.put("diaChiDisplay", customer.getDiaChiDisplay());

        return data;
    }

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create excel.
    private byte[] createExcel(
            List<NhanVien> employees
    ) throws IOException {
        String[] headers = {
                "STT",
                "Mã NV",
                "Họ tên",
                "Email",
                "SĐT",
                "CCCD",
                "Giới tính",
                "Ngày sinh",
                "Ngày vào làm",
                "Địa chỉ",
                "Vai trò",
                "Trạng thái"
        };

        DateTimeFormatter format =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Nhân viên");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            Row header = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < employees.size(); rowIndex++) {
                NhanVien employee = employees.get(rowIndex);

                Row row = sheet.createRow(rowIndex + 1);

                String[] values = {
                        String.valueOf(rowIndex + 1),
                        value(employee.getMaNv()),
                        value(employee.getHoTen()),
                        value(employee.getEmail()),
                        value(employee.getSoDienThoai()),
                        value(employee.getCccd()),
                        value(employee.getGioiTinh()),
                        employee.getNgaySinh() == null
                                ? ""
                                : employee.getNgaySinh().format(format),
                        employee.getNgayVaoLam() == null
                                ? ""
                                : employee.getNgayVaoLam().format(format),
                        value(employee.getDiaChiDisplay()),
                        value(employee.getVaiTro()),
                        Boolean.TRUE.equals(employee.getTrangThai())
                                ? "Đang làm"
                                : "Nghỉ việc"
                };

                for (int column = 0; column < values.length; column++) {
                    Cell cell = row.createCell(column);
                    cell.setCellValue(values[column]);
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(output);

            return output.toByteArray();
        }
    }

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create header style.
    private CellStyle createHeaderStyle(
            Workbook workbook
    ) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);

        style.setFont(font);
        style.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex()
        );
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create data style.
    private CellStyle createDataStyle(
            Workbook workbook
    ) {
        CellStyle style = workbook.createCellStyle();

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    // Thực hiện xử lý nghiệp vụ của hàm value.
    private String value(String value) {
        return value == null ? "" : value;
    }
}
