package com.example.sp.controller.hoadon;

import com.example.sp.dto.hoadon.HoaDonChiTietDTO;
import com.example.sp.dto.hoadon.CapNhatHoaDonRequest;
import com.example.sp.dto.hoadon.HoaDonTrangThaiResponse;
import com.example.sp.dto.hoadon.XacNhanHoanHangRequest;
import com.example.sp.dto.hoadon.XuLyGiaoHangThatBaiRequest;
import com.example.sp.dto.hoadon.YeuCauHoanHangRequest;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.LichSuThanhToan;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import com.example.sp.service.hoadon.HoaDonService;
import com.example.sp.validation.CustomerNameValidator;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/hoa-don")
@RequiredArgsConstructor
public class HoaDonController {

    private final HoaDonService hoaDonService;

    // =====================================================
    // 16. LẤY GIÁ HÓA ĐƠN CAO NHẤT (cho slider khoảng giá)
    // =====================================================
    @GetMapping("/api/gia-max")
    // Tải hoặc truy xuất dữ liệu cho get gia max.
    public BigDecimal getGiaMax() {
        return hoaDonService.getGiaMax();
    }

    // =====================================================
    // 1. LIST + FILTER + PAGING
    // =====================================================
    @GetMapping(value = "/api", produces = "application/json;charset=UTF-8")
    // Tải hoặc truy xuất dữ liệu cho get all.
    public Page<HoaDon> getAll(
            @RequestParam(required = false) String maHD,
            @RequestParam(required = false) String tuNgay,
            @RequestParam(required = false) String denNgay,
            @RequestParam(required = false) String loaiDon,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) BigDecimal maxGia,
            Pageable pageable
    ) {
        return hoaDonService.timKiem(maHD, tuNgay, denNgay, loaiDon, trangThai, maxGia, pageable);
    }

    // =====================================================
    // 2. GET DETAIL HÓA ĐƠN
    // =====================================================
    @GetMapping("/{id}")
    // Tải hoặc truy xuất dữ liệu cho get by id.
    public HoaDon getById(@PathVariable Integer id) {
        return hoaDonService.findById(id);
    }

    // =====================================================
    // 3. GET CHI TIẾT SẢN PHẨM
    // =====================================================
    @GetMapping("/{id}/chi-tiet")
    // Tải hoặc truy xuất dữ liệu cho get chi tiet.
    public List<HoaDonChiTietDTO> getChiTiet(@PathVariable Integer id) {
        return hoaDonService.getChiTiet(id);
    }

    // =====================================================
    // 4. GET LỊCH SỬ THANH TOÁN
    // =====================================================
    @GetMapping("/{id}/lich-su")
    // Tải hoặc truy xuất dữ liệu cho get lich su.
    public List<LichSuThanhToan> getLichSu(@PathVariable Integer id) {
        return hoaDonService.getLichSu(id);
    }

    // =====================================================
    // 5. TẠO HÓA ĐƠN
    // =====================================================
    @PostMapping("/tao")
    // Thực hiện xử lý nghiệp vụ của hàm tao hoa don.
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
    // Thực hiện xử lý nghiệp vụ của hàm them san pham.
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
    // Thực hiện xử lý nghiệp vụ của hàm cap nhat so luong.
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
    // Thực hiện xử lý nghiệp vụ của hàm xoa.
    public void xoa(@PathVariable Integer idHdct) {
        hoaDonService.xoaSanPham(idHdct);
    }

    // =====================================================
    // 9. TÍNH TỔNG TIỀN
    // =====================================================
    @GetMapping("/{id}/tong-tien")
    // Thực hiện xử lý nghiệp vụ của hàm tong tien.
    public BigDecimal tongTien(@PathVariable Integer id) {
        return hoaDonService.tinhTongTien(id);
    }

    // =====================================================
    // 10. ÁP VOUCHER
    // =====================================================
    @PostMapping("/{id}/voucher")
    // Thực hiện xử lý nghiệp vụ của hàm ap voucher.
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
    // Thực hiện xử lý nghiệp vụ của hàm thanh toan.
    public HoaDonTrangThaiResponse thanhToan(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "Tiền mặt") String hinhThucThanhToan
    ) {
        return HoaDonTrangThaiResponse.from(hoaDonService.thanhToan(id, hinhThucThanhToan));
    }

    // =====================================================
    // 12. HỦY HÓA ĐƠN
    // =====================================================
    @PostMapping("/{id}/huy")
    // Thực hiện xử lý nghiệp vụ của hàm huy.
    public HoaDonTrangThaiResponse huy(@PathVariable Integer id) {
        return HoaDonTrangThaiResponse.from(hoaDonService.huyHoaDon(id));
    }

    @PostMapping("/{id}/cho-hang-hoan")
    // Thực hiện xử lý nghiệp vụ của hàm cho hang hoan.
    public HoaDonTrangThaiResponse choHangHoan(
            @PathVariable Integer id,
            @RequestBody(required = false) YeuCauHoanHangRequest request
    ) {
        String lyDo = request == null ? null : request.getLyDo();
        return HoaDonTrangThaiResponse.from(hoaDonService.yeuCauHoanHang(id, lyDo));
    }

    @PostMapping("/{id}/giao-hang-that-bai")
    // Thực hiện xử lý nghiệp vụ của hàm giao hang that bai.
    public HoaDonTrangThaiResponse giaoHangThatBai(
            @PathVariable Integer id,
            @RequestBody XuLyGiaoHangThatBaiRequest request
    ) {
        return HoaDonTrangThaiResponse.from(hoaDonService.xuLyGiaoHangThatBai(id, request));
    }

    @PostMapping("/{id}/xac-nhan-hang-hoan")
    // Thực hiện xử lý nghiệp vụ của hàm xac nhan hang hoan.
    public HoaDonTrangThaiResponse xacNhanHangHoan(
            @PathVariable Integer id,
            @RequestBody XacNhanHoanHangRequest request
    ) {
        return HoaDonTrangThaiResponse.from(hoaDonService.xacNhanHangHoan(id, request));
    }

    // =====================================================
    // 13. XUẤT EXCEL THEO BỘ LỌC
    // =====================================================
    @GetMapping("/api/xuat-excel")
    // Thực hiện xử lý nghiệp vụ của hàm xuat excel.
    public org.springframework.http.ResponseEntity<byte[]> xuatExcel(
            @RequestParam(required = false) String maHD,
            @RequestParam(required = false) String tuNgay,
            @RequestParam(required = false) String denNgay,
            @RequestParam(required = false) String loaiDon,
            @RequestParam(required = false) String trangThai
    ) {
        try {
            Page<HoaDon> pageData = hoaDonService.timKiem(maHD, tuNgay, denNgay, loaiDon, trangThai, null, org.springframework.data.domain.Pageable.unpaged());
            List<HoaDon> danhSach = pageData.getContent();

            try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Danh sách Hóa đơn");
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                org.apache.poi.ss.usermodel.CellStyle headerCellStyle = workbook.createCellStyle();
                headerCellStyle.setFont(headerFont);

                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                String[] columns = {"STT", "Mã HD", "Tên NV", "Tên KH", "SĐT KH", "Tổng Tiền TT", "Loại Đơn", "Ngày Tạo", "Trạng Thái"};
                for (int i = 0; i < columns.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerCellStyle);
                }

                int rowIdx = 1;
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
                for (HoaDon hd : danhSach) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(rowIdx - 1);
                    row.createCell(1).setCellValue(hd.getMaHoaDon() != null ? hd.getMaHoaDon() : "");
                    row.createCell(2).setCellValue(employeeRole(hd));
                    row.createCell(3).setCellValue(hd.getTenKhachHang() != null ? hd.getTenKhachHang() : "Khách lẻ");
                    row.createCell(4).setCellValue(hd.getSoDienThoai() != null ? hd.getSoDienThoai() : "");
                    row.createCell(5).setCellValue(hd.getTongTienThanhToan() != null ? hd.getTongTienThanhToan().doubleValue() : 0.0);
                    String orderType = "Giao hàng".equalsIgnoreCase(hd.getLoaiDon())
                            ? "Tại quầy"
                            : (hd.getLoaiDon() != null ? hd.getLoaiDon() : "");
                    row.createCell(6).setCellValue(orderType);
                    row.createCell(7).setCellValue(hd.getNgayTao() != null ? hd.getNgayTao().format(formatter) : "");
                    row.createCell(8).setCellValue(hd.getTrangThai() != null ? hd.getTrangThai() : "");
                }
                for (int i = 0; i < columns.length; i++) { sheet.autoSizeColumn(i); }

                workbook.write(out);
                byte[] bytes = out.toByteArray();
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
    // 14. UPDATE THÔNG TIN KHÁCH HÀNG
    // =====================================================
    @PutMapping("/update-thong-tin/{id}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update thong tin hoa don.
    public ResponseEntity<?> updateThongTinHoaDon(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {

        String newTenKH = body.get("tenKhachHang");
        String newSdtKH = body.get("soDienThoai");

        newTenKH = CustomerNameValidator.normalize(newTenKH);
        if (newTenKH == null || newTenKH.isEmpty()) {
            return ResponseEntity.badRequest().body("Tên khách hàng không được để trống!");
        }
        if (!CustomerNameValidator.isValid(newTenKH)) {
            return ResponseEntity.badRequest().body(
                    CustomerNameValidator.INVALID_MESSAGE
            );
        }
        if (newSdtKH != null && !newSdtKH.trim().isEmpty()) {
            if (!newSdtKH.matches("^(03|05|07|08|09)\\d{8}$")) {
                return ResponseEntity.badRequest().body("Số điện thoại không đúng định dạng Việt Nam!");
            }
        }

        try {
            boolean isUpdated = hoaDonService.updateThongTinKhachHang(id, newTenKH, newSdtKH);
            if (isUpdated) {
                return ResponseEntity.ok().body("Cập nhật thành công!");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Có lỗi xảy ra!");
        }
    }

    @PutMapping("/{id}/chinh-sua")
    // Thực hiện xử lý nghiệp vụ của hàm cap nhat hoa don cho xu ly.
    public ResponseEntity<Void> capNhatHoaDonChoXuLy(
            @PathVariable Integer id,
            @Valid @RequestBody CapNhatHoaDonRequest request
    ) {
        hoaDonService.capNhatHoaDonChoXuLy(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/don-mua-them")
    // Thực hiện xử lý nghiệp vụ của hàm tao don mua them.
    public ResponseEntity<Map<String, Object>> taoDonMuaThem(@PathVariable Integer id) {
        HoaDon created = hoaDonService.taoDonMuaThem(id);
        return ResponseEntity.status(201).body(Map.of(
                "id", created.getId(),
                "maHoaDon", created.getMaHoaDon()
        ));
    }

    @PostMapping("/{id}/don-giao-lai")
    // Thực hiện xử lý nghiệp vụ của hàm tao don giao lai do mat hang.
    public ResponseEntity<Map<String, Object>> taoDonGiaoLaiDoMatHang(@PathVariable Integer id) {
        HoaDon created = hoaDonService.taoDonGiaoLaiDoMatHang(id);
        return ResponseEntity.status(201).body(Map.of(
                "id", created.getId(),
                "maHoaDon", created.getMaHoaDon()
        ));
    }

    @PostMapping("/{id}/xac-nhan-den-bu-van-chuyen")
    // Thực hiện xử lý nghiệp vụ của hàm xac nhan don vi van chuyen den bu.
    public HoaDonTrangThaiResponse xacNhanDonViVanChuyenDenBu(@PathVariable Integer id) {
        return HoaDonTrangThaiResponse.from(hoaDonService.xacNhanDonViVanChuyenDenBu(id));
    }

    // =====================================================
    // 15. CẬP NHẬT TRẠNG THÁI
    // =====================================================
    @PutMapping("/{id}/trang-thai")
    // Thực hiện xử lý nghiệp vụ của hàm cap nhat trang thai.
    public ResponseEntity<?> capNhatTrangThai(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            HttpSession session
    ) {
        String trangThai = body.get("trangThai");
        Integer idNhanVien = (Integer) session.getAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
        HoaDon hd = hoaDonService.capNhatTrangThai(id, trangThai, idNhanVien);
        return ResponseEntity.ok(HoaDonTrangThaiResponse.from(hd));
    }

    // Thực hiện xử lý nghiệp vụ của hàm employee role.
    private String employeeRole(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getNhanVien() == null) {
            return "";
        }
        String role = hoaDon.getNhanVien().getVaiTro();
        if (role == null || role.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(role.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .replace('_', ' ')
                .replace('-', ' ')
                .toLowerCase(Locale.ROOT);
        if (normalized.contains("quan") || normalized.contains("admin") || normalized.contains("manager")) {
            return "Quản lý";
        }
        if (normalized.contains("nhan") || normalized.contains("staff") || normalized.contains("employee")) {
            return "Nhân viên";
        }
        return role.trim();
    }
}
