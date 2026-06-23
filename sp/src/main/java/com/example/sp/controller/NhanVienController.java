package com.example.sp.controller;

import com.example.sp.model.employee.NhanVien;
import com.example.sp.service.employee.NhanVienService;
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
import java.util.List;

@RestController
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienController {

    private final NhanVienService nhanVienService;

    @GetMapping
    public Page<NhanVien> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String vaiTro,
            @RequestParam(required = false) Boolean trangThai,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return nhanVienService.getAll(keyword, vaiTro, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public NhanVien getById(@PathVariable Integer id) {
        return nhanVienService.findById(id);
    }

    @PostMapping
    public NhanVien create(@RequestBody NhanVien nhanVien) {
        nhanVien.setId(null);
        return nhanVienService.save(nhanVien);
    }

    @PutMapping("/{id}")
    public NhanVien update(@PathVariable Integer id, @RequestBody NhanVien nhanVien) {
        nhanVien.setId(id);
        return nhanVienService.save(nhanVien);
    }

    @PatchMapping("/{id}/trang-thai")
    public NhanVien toggleStatus(@PathVariable Integer id) {
        NhanVien nhanVien = nhanVienService.findById(id);
        nhanVien.setTrangThai(!Boolean.TRUE.equals(nhanVien.getTrangThai()));
        return nhanVienService.save(nhanVien);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        nhanVienService.delete(id);
    }

    @GetMapping("/xuat-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String vaiTro,
            @RequestParam(required = false) Boolean trangThai
    ) throws IOException {
        List<NhanVien> danhSach = nhanVienService.getAll(keyword, vaiTro, trangThai, Pageable.unpaged()).getContent();
        byte[] excelBytes = exportToExcel(danhSach);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "danh_sach_nhan_vien.xlsx");
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    private byte[] exportToExcel(List<NhanVien> danhSach) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Danh sach nhan vien");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dataStyle = dataStyle(workbook);

            String[] headers = {"STT", "Ma NV", "Ho ten", "Email", "SDT", "Dia chi", "Vai tro", "Gioi tinh", "Ngay sinh", "CCCD", "Ngay vao lam", "Trang thai"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (NhanVien nv : danhSach) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(value(nv.getMaNv()));
                row.createCell(2).setCellValue(value(nv.getHoTen()));
                row.createCell(3).setCellValue(value(nv.getEmail()));
                row.createCell(4).setCellValue(value(nv.getSoDienThoai()));
                row.createCell(5).setCellValue(value(nv.getDiaChi()));
                row.createCell(6).setCellValue(value(nv.getVaiTro()));
                row.createCell(7).setCellValue(value(nv.getGioiTinh()));
                row.createCell(8).setCellValue(nv.getNgaySinh() == null ? "" : nv.getNgaySinh().format(dateFormatter));
                row.createCell(9).setCellValue(value(nv.getCccd()));
                row.createCell(10).setCellValue(nv.getNgayVaoLam() == null ? "" : nv.getNgayVaoLam().format(dateFormatter));
                row.createCell(11).setCellValue(Boolean.TRUE.equals(nv.getTrangThai()) ? "Dang lam" : "Nghi viec");

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle dataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String value(String text) {
        return text == null ? "" : text;
    }
}
