package com.example.sp.controller;

import com.example.sp.model.customer.KhachHang;
import com.example.sp.service.customer.KhachHangService;
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
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {

    private final KhachHangService khachHangService;

    @GetMapping
    public Page<KhachHang> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean trangThai,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return khachHangService.getAll(keyword, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public KhachHang getById(@PathVariable Integer id) {
        return khachHangService.findById(id);
    }

    @PostMapping
    public KhachHang create(@RequestBody KhachHang khachHang) {
        khachHang.setId(null);
        return khachHangService.save(khachHang);
    }

    @PutMapping("/{id}")
    public KhachHang update(@PathVariable Integer id, @RequestBody KhachHang khachHang) {
        khachHang.setId(id);
        return khachHangService.save(khachHang);
    }

    @PatchMapping("/{id}/trang-thai")
    public KhachHang toggleStatus(@PathVariable Integer id) {
        KhachHang khachHang = khachHangService.findById(id);
        khachHang.setTrangThai(!Boolean.TRUE.equals(khachHang.getTrangThai()));
        return khachHangService.save(khachHang);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        khachHangService.delete(id);
    }

    @GetMapping("/xuat-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean trangThai
    ) throws IOException {
        List<KhachHang> danhSach = khachHangService.getAll(keyword, trangThai, Pageable.unpaged()).getContent();
        byte[] excelBytes = exportToExcel(danhSach);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "danh_sach_khach_hang.xlsx");
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    private byte[] exportToExcel(List<KhachHang> danhSach) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Danh sach khach hang");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dataStyle = dataStyle(workbook);

            String[] headers = {"STT", "Ma KH", "Ten khach hang", "Email", "SDT", "CCCD",
                    "Dia chi chi tiet", "Phuong/Xa", "Quan/Huyen", "Tinh/Thanh",
                    "Gioi tinh", "Ngay sinh", "Trang thai"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (KhachHang kh : danhSach) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(value(kh.getMaKh()));
                row.createCell(2).setCellValue(value(kh.getTenKhachHang()));
                row.createCell(3).setCellValue(value(kh.getEmail()));
                row.createCell(4).setCellValue(value(kh.getSoDienThoai()));
                row.createCell(5).setCellValue(value(kh.getCccd()));
                row.createCell(6).setCellValue(value(kh.getDiaChiChiTiet()));
                row.createCell(7).setCellValue(value(kh.getPhuongXa()));
                row.createCell(8).setCellValue(value(kh.getQuanHuyen()));
                row.createCell(9).setCellValue(value(kh.getTinhThanh()));
                row.createCell(10).setCellValue(value(kh.getGioiTinh()));
                row.createCell(11).setCellValue(kh.getNgaySinh() == null ? "" : kh.getNgaySinh().format(dateFormatter));
                row.createCell(12).setCellValue(Boolean.TRUE.equals(kh.getTrangThai()) ? "Hoat dong" : "Ngung hoat dong");

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