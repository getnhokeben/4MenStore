package com.example.sp.controller.sanpham;

import com.example.sp.cauhinh.UploadStorageProperties;
import com.example.sp.service.tienich.MoneyRoundingUtil;
import com.example.sp.dto.sanpham.ChiTietSanPhamUpdateRequest;
import com.example.sp.dto.sanpham.SanPhamFullRequest;
import com.example.sp.model.sanpham.ChatLieu;
import com.example.sp.model.sanpham.ChiTietSanPham;
import com.example.sp.model.sanpham.KichCo;
import com.example.sp.model.sanpham.KieuDang;
import com.example.sp.model.sanpham.LoaiAo;
import com.example.sp.model.sanpham.MauSac;
import com.example.sp.model.sanpham.PhongCachMac;
import com.example.sp.model.sanpham.SanPham;
import com.example.sp.model.sanpham.XuatXu;
import com.example.sp.repository.sanpham.ChatLieuRepository;
import com.example.sp.repository.sanpham.KichCoRepository;
import com.example.sp.repository.sanpham.KieuDangRepository;
import com.example.sp.repository.sanpham.LoaiAoRepository;
import com.example.sp.repository.sanpham.MauSacRepository;
import com.example.sp.repository.sanpham.PhongCachMacRepository;
import com.example.sp.repository.sanpham.SanPhamRepository;
import com.example.sp.repository.sanpham.XuatXuRepository;
import com.example.sp.service.sanpham.SanPhamService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/san-pham")
public class SanPhamController {

    private static final Logger log = LoggerFactory.getLogger(SanPhamController.class);
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    @Autowired private SanPhamRepository sanPhamRepo;
    @Autowired private SanPhamService sanPhamService;
    @Autowired private KichCoRepository kichCoRepo;
    @Autowired private MauSacRepository mauSacRepo;
    @Autowired private LoaiAoRepository loaiAoRepo;
    @Autowired private PhongCachMacRepository phongCachMacRepo;
    @Autowired private KieuDangRepository kieuDangRepo;
    @Autowired private XuatXuRepository xuatXuRepo;
    @Autowired private ChatLieuRepository chatLieuRepo;
    @Autowired private UploadStorageProperties uploadStorageProperties;

    @GetMapping(value = "/trang-chu", produces = MediaType.TEXT_HTML_VALUE)
    // Thực hiện xử lý nghiệp vụ của hàm trang chu.
    public ResponseEntity<byte[]> trangChu() throws IOException {
        return html("templates/TrangChu.html");
    }

    @GetMapping(value = "/quan-ly", produces = MediaType.TEXT_HTML_VALUE)
    // Thực hiện xử lý nghiệp vụ của hàm quan ly.
    public ResponseEntity<byte[]> quanLy() throws IOException {
        return html("templates/QuanLySanPham.html");
    }

    @GetMapping(value = "/bien-the", produces = MediaType.TEXT_HTML_VALUE)
    // Thực hiện xử lý nghiệp vụ của hàm bien the san pham.
    public ResponseEntity<byte[]> bienTheSanPham() throws IOException {
        return html("templates/BienTheSanPham.html");
    }

    // Thực hiện xử lý nghiệp vụ của hàm html.
    private ResponseEntity<byte[]> html(String path) throws IOException {
        byte[] html = new ClassPathResource(path).getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    // Thực hiện xử lý nghiệp vụ của hàm product result.
    private Map<String, Object> productResult(SanPham product, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", message);
        result.put("idSp", product.getIdSp());
        result.put("maSp", product.getMaSp());
        result.put("tenSp", product.getTenSp());
        return result;
    }

    // Thực hiện xử lý nghiệp vụ của hàm variant result.
    private Map<String, Object> variantResult(ChiTietSanPham variant, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", message);
        result.put("idSpct", variant.getIdSpct());
        result.put("idSanPham", variant.getIdSanPham());
        result.put("maChiTietSanPham", variant.getMaChiTietSanPham());
        result.put("soLuongTon", variant.getSoLuongTon());
        result.put("soLuongGiu", variant.getSoLuongGiu());
        result.put("soLuongKhaDung", variant.getSoLuongKhaDung());
        result.put("donGia", MoneyRoundingUtil.roundNonNegative(variant.getDonGia()));
        result.put("trangThai", variant.getTrangThai());
        return result;
    }

    @GetMapping("/hien-thi")
    // Tải hoặc truy xuất dữ liệu cho get all.
    public ResponseEntity<Page<SanPham>> getAll(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "chatLieu", required = false) String chatLieu,
            @RequestParam(value = "trangThai", required = false) Boolean trangThai,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        return ResponseEntity.ok(sanPhamService.getProducts(keyword, chatLieu, trangThai, page, size));
    }

    @GetMapping("/bien-the/{id}")
    // Tải hoặc truy xuất dữ liệu cho get variants.
    public ResponseEntity<List<ChiTietSanPham>> getVariants(@PathVariable("id") Integer idSp) {
        return ResponseEntity.ok(sanPhamService.getProductVariants(idSp));
    }

    @PutMapping("/bien-the/{id}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update variant.
    public ResponseEntity<?> updateVariant(@PathVariable("id") Integer idSpct,
                                           @Valid @RequestBody ChiTietSanPhamUpdateRequest request) {
        try {
            ChiTietSanPham updated = sanPhamService.updateVariant(idSpct, request);
            return ResponseEntity.ok(variantResult(updated, "Cập nhật biến thể thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/them")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create product.
    public ResponseEntity<?> createProduct(@Valid @RequestBody SanPhamFullRequest request) {
        try {
            SanPham created = sanPhamService.createProduct(request);
            return ResponseEntity.status(201).body(productResult(created, "Thêm sản phẩm thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/upload-anh", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // Thực hiện xử lý nghiệp vụ của hàm upload image.
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn file ảnh");
            }

            if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
                return ResponseEntity.badRequest().body("Ảnh không được vượt quá 5 MB");
            }

            String extension = detectSafeImageExtension(file);
            if (extension == null) {
                return ResponseEntity.badRequest().body("Chỉ chấp nhận ảnh JPG, PNG hoặc GIF hợp lệ");
            }

            String fileName = UUID.randomUUID() + "." + extension;
            Path uploadDir = uploadStorageProperties.directoryPath();
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(fileName).normalize();
            if (!target.startsWith(uploadDir.toAbsolutePath().normalize())) {
                return ResponseEntity.badRequest().body("Tên tệp không hợp lệ");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Map<String, String> result = new HashMap<>();
            result.put("url", "/uploads/" + fileName);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("uploadImage failed", e);
            return ResponseEntity.status(500).body("Không thể upload ảnh");
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm detect safe image extension.
    private String detectSafeImageExtension(MultipartFile file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) return null;

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;

            ImageReader reader = readers.next();
            try {
                return switch (reader.getFormatName().toLowerCase(java.util.Locale.ROOT)) {
                    case "jpeg", "jpg" -> "jpg";
                    case "png" -> "png";
                    case "gif" -> "gif";
                    default -> null;
                };
            } finally {
                reader.dispose();
            }
        }
    }

    @PutMapping("/sua/{id}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update product.
    public ResponseEntity<?> updateProduct(@PathVariable("id") Integer idSp,
                                           @Valid @RequestBody SanPhamFullRequest request) {
        try {
            SanPham updated = sanPhamService.updateProduct(idSp, request);
            return ResponseEntity.ok(productResult(updated, "Cập nhật sản phẩm thành công"));
        } catch (RuntimeException e) {
            log.warn("updateProduct validation failed for id={}: {}", idSp, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("updateProduct failed for id={}", idSp, e);
            return ResponseEntity.status(500).body("Không thể cập nhật sản phẩm");
        }
    }

    @DeleteMapping("/xoa/{id}")
    // Xử lý thao tác đóng, xóa hoặc hủy cho delete product.
    public ResponseEntity<String> deleteProduct(@PathVariable("id") Integer idSp) {
        try {
            sanPhamService.softDeleteProduct(idSp);
            return ResponseEntity.ok("Xóa mềm sản phẩm và biến thể thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/trang-thai/{id}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update status.
    public ResponseEntity<?> updateStatus(@PathVariable("id") Integer idSp,
                                          @RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("trangThai")) {
                return ResponseEntity.badRequest().body("Thiếu trường trangThai");
            }
            Boolean newStatus = Boolean.valueOf(String.valueOf(body.get("trangThai")));
            sanPhamService.setProductStatus(idSp, newStatus);
            return ResponseEntity.ok("Trạng thái sản phẩm đã được cập nhật");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("updateStatus failed for id={}", idSp, e);
            return ResponseEntity.status(500).body("Không thể cập nhật trạng thái sản phẩm");
        }
    }

    @GetMapping("/phan-trang")
    // Thực hiện xử lý nghiệp vụ của hàm phan trang.
    public ResponseEntity<List<SanPham>> phanTrang(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber) {
        int pageSize = 5;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ResponseEntity.ok(sanPhamRepo.findAll(pageable).getContent());
    }

    @GetMapping("/tim-kiem")
    // Thực hiện xử lý nghiệp vụ của hàm tim kiem.
    public ResponseEntity<List<SanPham>> timKiem(@RequestParam(name = "ten") String ten) {
        return ResponseEntity.ok(sanPhamRepo.findSanPhamsByTenSpContains(ten));
    }

    @GetMapping("/danh-muc/kich-co")
    // Thực hiện xử lý nghiệp vụ của hàm danh muc kich co.
    public ResponseEntity<List<KichCo>> danhMucKichCo() {
        return ResponseEntity.ok(kichCoRepo.findAll(Sort.by("idKichCo").ascending()));
    }

    @GetMapping("/danh-muc/mau-sac")
    // Thực hiện xử lý nghiệp vụ của hàm danh muc mau sac.
    public ResponseEntity<List<MauSac>> danhMucMauSac() {
        return ResponseEntity.ok(mauSacRepo.findAll(Sort.by("idMauSac").ascending()));
    }

    @GetMapping("/danh-muc/loai-ao")
    // Thực hiện xử lý nghiệp vụ của hàm danh muc loai ao.
    public ResponseEntity<List<LoaiAo>> danhMucLoaiAo() {
        return ResponseEntity.ok(loaiAoRepo.findAll(Sort.by("idLoaiAo").ascending()));
    }

    @GetMapping({"/danh-muc/phong-cach", "/danh-muc/phong-cach-mac"})
    // Thực hiện xử lý nghiệp vụ của hàm danh muc phong cach.
    public ResponseEntity<List<PhongCachMac>> danhMucPhongCach() {
        return ResponseEntity.ok(phongCachMacRepo.findAll(Sort.by("idPhongCachMac").ascending()));
    }

    @GetMapping("/danh-muc/kieu-dang")
    // Thực hiện xử lý nghiệp vụ của hàm danh muc kieu dang.
    public ResponseEntity<List<KieuDang>> danhMucKieuDang() {
        return ResponseEntity.ok(kieuDangRepo.findAll(Sort.by("idKieuDang").ascending()));
    }

    @GetMapping("/danh-muc/xuat-xu")
    // Thực hiện xử lý nghiệp vụ của hàm danh muc xuat xu.
    public ResponseEntity<List<XuatXu>> danhMucXuatXu() {
        return ResponseEntity.ok(xuatXuRepo.findAll(Sort.by("idXuatXu").ascending()));
    }

    @GetMapping("/danh-muc/chat-lieu")
    // Thực hiện xử lý nghiệp vụ của hàm danh muc chat lieu.
    public ResponseEntity<List<ChatLieu>> danhMucChatLieu() {
        return ResponseEntity.ok(chatLieuRepo.findAll(Sort.by("idChatLieu").ascending()));
    }

    @GetMapping("/{id}")
    // Tải hoặc truy xuất dữ liệu cho get by id.
    public ResponseEntity<SanPham> getById(@PathVariable Integer id) {
        return sanPhamRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
