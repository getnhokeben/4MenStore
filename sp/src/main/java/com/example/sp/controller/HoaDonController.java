package com.example.sp.controller;

import com.example.sp.dto.HoaDonChiTietDTO;
import com.example.sp.model.order.HoaDon;
import com.example.sp.model.order.LichSuThanhToan;
import com.example.sp.service.order.HoaDonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hoa-don")
@RequiredArgsConstructor
public class HoaDonController {

    private final HoaDonService hoaDonService;

    // =====================================================
    // 1. LIST + FILTER + PAGING (ĐÃ SỬA CHỮA TIẾNG VIỆT)
    // =====================================================
    @GetMapping(value = "/api", produces = "application/json;charset=UTF-8")
    public Page<HoaDon> getAll(
            @RequestParam(required = false) String maHD,
            @RequestParam(required = false) String tuNgay,
            @RequestParam(required = false) String denNgay,
            @RequestParam(required = false) String loaiDon,
            @RequestParam(required = false) String trangThai,
            Pageable pageable
    ) {
        return hoaDonService.timKiem(maHD, tuNgay, denNgay, loaiDon, trangThai, pageable);
    }

    // =====================================================
    // 2. GET DETAIL HÓA ĐƠN
    // =====================================================
    @GetMapping("/{id}")
    public HoaDon getById(@PathVariable Integer id) {
        return hoaDonService.findById(id);
    }

    // =====================================================
    // 3. GET CHI TIẾT SẢN PHẨM
    // =====================================================
    @GetMapping("/{id}/chi-tiet")
    public List<HoaDonChiTietDTO> getChiTiet(@PathVariable Integer id) {
        return hoaDonService.getChiTiet(id);
    }

    // =====================================================
    // 4. GET LỊCH SỬ THANH TOÁN
    // =====================================================
    @GetMapping("/{id}/lich-su")
    public List<LichSuThanhToan> getLichSu(@PathVariable Integer id) {
        return hoaDonService.getLichSu(id);
    }

    // =====================================================
    // 5. TẠO HÓA ĐƠN
    // =====================================================
    @PostMapping("/tao")
    public HoaDon taoHoaDon(
            @RequestParam(required = false) Integer idKh,
            @RequestParam(required = false) Integer idNv
    ) {
        return hoaDonService.taoHoaDon(idKh, idNv);
    }

    // =====================================================
    // 6. THÊM SẢN PHẨM
    // =====================================================
    @PostMapping("/{id}/them-san-pham")
    public void themSanPham(
            @PathVariable Integer id,
            @RequestParam Integer idSpct,
            @RequestParam Integer soLuong
    ) {
        hoaDonService.themSanPham(id, idSpct, soLuong);
    }

    // =====================================================
    // 7. UPDATE SỐ LƯỢNG
    // =====================================================
    @PutMapping("/chi-tiet/{idHdct}")
    public void capNhatSoLuong(
            @PathVariable Integer idHdct,
            @RequestParam Integer soLuong
    ) {
        hoaDonService.capNhatSoLuong(idHdct, soLuong);
    }

    // =====================================================
    // 8. XÓA SẢN PHẨM
    // =====================================================
    @DeleteMapping("/chi-tiet/{idHdct}")
    public void xoa(@PathVariable Integer idHdct) {
        hoaDonService.xoaSanPham(idHdct);
    }

    // =====================================================
    // 9. TÍNH TỔNG TIỀN
    // =====================================================
    @GetMapping("/{id}/tong-tien")
    public BigDecimal tongTien(@PathVariable Integer id) {
        return hoaDonService.tinhTongTien(id);
    }

    // =====================================================
    // 10. ÁP VOUCHER
    // =====================================================
    @PostMapping("/{id}/voucher")
    public HoaDon apVoucher(
            @PathVariable Integer id,
            @RequestParam Integer idVoucher
    ) {
        return hoaDonService.apVoucher(id, idVoucher);
    }

    // =====================================================
    // 11. THANH TOÁN
    // =====================================================
    @PostMapping("/{id}/thanh-toan")
    public HoaDon thanhToan(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "Tiền mặt") String hinhThucThanhToan
    ) {
        return hoaDonService.thanhToan(id, hinhThucThanhToan);
    }

    // =====================================================
    // 12. HỦY HÓA ĐƠN
    // =====================================================
    @PostMapping("/{id}/huy")
    public HoaDon huy(@PathVariable Integer id) {
        return hoaDonService.huyHoaDon(id);
    }

    // =====================================================
    // 13. XUẤT EXCEL THEO BỘ LỌC
    // =====================================================
    @GetMapping("/api/xuat-excel")
    public org.springframework.http.ResponseEntity<byte[]> xuatExcel(
            @RequestParam(required = false) String maHD,
            @RequestParam(required = false) String tuNgay,
            @RequestParam(required = false) String denNgay,
            @RequestParam(required = false) String loaiDon,
            @RequestParam(required = false) String trangThai
    ) {
        try {
            // Lấy toàn bộ danh sách không phân trang để xuất file (truyền Pageable.unpaged())
            Page<HoaDon> pageData = hoaDonService.timKiem(maHD, tuNgay, denNgay, loaiDon, trangThai, org.springframework.data.domain.Pageable.unpaged());
            List<HoaDon> danhSach = pageData.getContent();

            // Tạo Workbook Excel
            try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Danh sách Hóa đơn");

                // Tạo font và kiểu dáng cho Tiêu đề dòng (Header)
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                org.apache.poi.ss.usermodel.CellStyle headerCellStyle = workbook.createCellStyle();
                headerCellStyle.setFont(headerFont);

                // Tạo hàng Tiêu đề (Row 0)
                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                String[] columns = {"STT", "Mã HD", "Tên NV", "Tên KH", "SĐT KH", "Tổng Tiền TT", "Loại Đơn", "Ngày Tạo", "Trạng Thái"};
                for (int i = 0; i < columns.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerCellStyle);
                }

                // Ghi dữ liệu hóa đơn vào các dòng tiếp theo
                int rowIdx = 1;
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

                for (HoaDon hd : danhSach) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);

                    row.createCell(0).setCellValue(rowIdx - 1); // STT
                    row.createCell(1).setCellValue(hd.getMaHoaDon() != null ? hd.getMaHoaDon() : "");
                    row.createCell(2).setCellValue(hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Nguyễn Văn An"); // Snapshot hoặc Lazy load tạm
                    row.createCell(3).setCellValue(hd.getTenKhachHang() != null ? hd.getTenKhachHang() : "Khách lẻ");
                    row.createCell(4).setCellValue(hd.getSoDienThoai() != null ? hd.getSoDienThoai() : "");
                    row.createCell(5).setCellValue(hd.getTongTienThanhToan() != null ? hd.getTongTienThanhToan().doubleValue() : 0.0);
                    row.createCell(6).setCellValue(hd.getLoaiDon() != null ? hd.getLoaiDon() : "");
                    row.createCell(7).setCellValue(hd.getNgayTao() != null ? hd.getNgayTao().format(formatter) : "");
                    row.createCell(8).setCellValue(hd.getTrangThai() != null ? hd.getTrangThai() : "");
                }

                // Tự động căn chỉnh độ rộng cột
                for (int i = 0; i < columns.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(out);
                byte[] bytes = out.toByteArray();

                // Trả file về cho trình duyệt tải xuống
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.add("Content-Disposition", "attachment; filename=danh_sach_hoa_don.xlsx");
                headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

                return new org.springframework.http.ResponseEntity<>(bytes, headers, org.springframework.http.HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // =====================================================
    // 14. UPDATE THÔNG TIN KHÁCH HÀNG (ĐÃ TỐI ƯU SANG SERVICE)
    // =====================================================
    @PutMapping("/update-thong-tin/{id}")
    public ResponseEntity<?> updateThongTinHoaDon(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {

        String newTenKH = body.get("tenKhachHang");
        String newSdtKH = body.get("soDienThoai");

        // Validate dữ liệu ở Backend đầu vào
        if (newTenKH == null || newTenKH.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Tên khách hàng không được để trống!");
        }
        if (newSdtKH != null && !newSdtKH.trim().isEmpty()) {
            if (!newSdtKH.matches("^(03|05|07|08|09)\\d{8}$")) {
                return ResponseEntity.badRequest().body("Số điện thoại không đúng định dạng Việt Nam!");
            }
        }

        try {
            // Đẩy phần logic tìm kiếm và lưu trữ xuống Service xử lý
            boolean isUpdated = hoaDonService.updateThongTinKhachHang(id, newTenKH, newSdtKH);
            if (isUpdated) {
                return ResponseEntity.ok().body("Cập nhật thông tin khách hàng trên hóa đơn thành công!");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Có lỗi xảy ra tại hệ thống!");
        }
    }

    @PutMapping("/{id}/trang-thai")
    public ResponseEntity<?> capNhatTrangThai(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body
    ) {

        String trangThai = body.get("trangThai");

        HoaDon hd = hoaDonService.capNhatTrangThai(id, trangThai);

        return ResponseEntity.ok(hd);
    }
}