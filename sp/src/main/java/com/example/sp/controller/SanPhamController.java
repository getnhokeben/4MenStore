package com.example.sp.controller;

import com.example.sp.dto.ChiTietSanPhamUpdateRequest;
import com.example.sp.dto.SanPhamFullRequest;
import com.example.sp.model.ChatLieu;
import com.example.sp.model.ChiTietSanPham;
import com.example.sp.model.KichCo;
import com.example.sp.model.KieuDang;
import com.example.sp.model.LoaiAo;
import com.example.sp.model.MauSac;
import com.example.sp.model.PhongCachMac;
import com.example.sp.model.SanPham;
import com.example.sp.model.XuatXu;
import com.example.sp.repository.ChatLieuRepository;
import com.example.sp.repository.KichCoRepository;
import com.example.sp.repository.KieuDangRepository;
import com.example.sp.repository.LoaiAoRepository;
import com.example.sp.repository.MauSacRepository;
import com.example.sp.repository.PhongCachMacRepository;
import com.example.sp.repository.SanPhamRepository;
import com.example.sp.repository.XuatXuRepository;
import com.example.sp.service.SanPhamService;
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
import org.springframework.web.bind.annotation.CrossOrigin;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/san-pham")
@CrossOrigin(origins = "*")
public class SanPhamController {

    private static final Logger log = LoggerFactory.getLogger(SanPhamController.class);

    @Autowired private SanPhamRepository sanPhamRepo;
    @Autowired private SanPhamService sanPhamService;
    @Autowired private KichCoRepository kichCoRepo;
    @Autowired private MauSacRepository mauSacRepo;
    @Autowired private LoaiAoRepository loaiAoRepo;
    @Autowired private PhongCachMacRepository phongCachMacRepo;
    @Autowired private KieuDangRepository kieuDangRepo;
    @Autowired private XuatXuRepository xuatXuRepo;
    @Autowired private ChatLieuRepository chatLieuRepo;

    @GetMapping(value = "/trang-chu", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> trangChu() throws IOException {
        return html("templates/TrangChu.html");
    }

    @GetMapping(value = "/quan-ly", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> quanLy() throws IOException {
        return html("templates/QuanLySanPham.html");
    }

    @GetMapping(value = "/bien-the", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> bienTheSanPham() throws IOException {
        return html("templates/BienTheSanPham.html");
    }

    private ResponseEntity<byte[]> html(String path) throws IOException {
        byte[] html = new ClassPathResource(path).getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    @GetMapping("/hien-thi")
    public ResponseEntity<Page<SanPham>> getAll(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "chatLieu", required = false) String chatLieu,
            @RequestParam(value = "trangThai", required = false) Boolean trangThai,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        return ResponseEntity.ok(sanPhamService.getProducts(keyword, chatLieu, trangThai, page, size));
    }

    @GetMapping("/bien-the/{id}")
    public ResponseEntity<List<ChiTietSanPham>> getVariants(@PathVariable("id") Integer idSp) {
        return ResponseEntity.ok(sanPhamService.getProductVariants(idSp));
    }

    @PutMapping("/bien-the/{id}")
    public ResponseEntity<?> updateVariant(@PathVariable("id") Integer idSpct,
                                           @Valid @RequestBody ChiTietSanPhamUpdateRequest request) {
        try {
            return ResponseEntity.ok(sanPhamService.updateVariant(idSpct, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/them")
    public ResponseEntity<?> createProduct(@Valid @RequestBody SanPhamFullRequest request) {
        try {
            SanPham created = sanPhamService.createProduct(request);
            return ResponseEntity.status(201).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/upload-anh", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn file ảnh");
            }

            String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
                extension = originalName.substring(dotIndex).replaceAll("[^a-zA-Z0-9.]", "");
            }
            String fileName = UUID.randomUUID() + extension;
            Path uploadDir = Path.of("uploads").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            Map<String, String> result = new HashMap<>();
            result.put("url", "/uploads/" + fileName);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("uploadImage failed", e);
            return ResponseEntity.status(500).body("Không thể upload ảnh");
        }
    }

    @PutMapping("/sua/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable("id") Integer idSp,
                                           @RequestBody SanPhamFullRequest request) {
        try {
            SanPham updated = sanPhamService.updateProduct(idSp, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.warn("updateProduct validation failed for id={}: {}", idSp, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("updateProduct failed for id={}", idSp, e);
            return ResponseEntity.status(500).body("Không thể cập nhật sản phẩm");
        }
    }

    @DeleteMapping("/xoa/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable("id") Integer idSp) {
        try {
            sanPhamService.softDeleteProduct(idSp);
            return ResponseEntity.ok("Xóa mềm sản phẩm và biến thể thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/trang-thai/{id}")
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
    public ResponseEntity<List<SanPham>> phanTrang(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber) {
        int pageSize = 5;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ResponseEntity.ok(sanPhamRepo.findAll(pageable).getContent());
    }

    @GetMapping("/tim-kiem")
    public ResponseEntity<List<SanPham>> timKiem(@RequestParam(name = "ten") String ten) {
        return ResponseEntity.ok(sanPhamRepo.findSanPhamsByTenSpContains(ten));
    }

    @GetMapping("/danh-muc/kich-co")
    public ResponseEntity<List<KichCo>> danhMucKichCo() {
        return ResponseEntity.ok(kichCoRepo.findAll(Sort.by("idKichCo").ascending()));
    }

    @GetMapping("/danh-muc/mau-sac")
    public ResponseEntity<List<MauSac>> danhMucMauSac() {
        return ResponseEntity.ok(mauSacRepo.findAll(Sort.by("idMauSac").ascending()));
    }

    @GetMapping("/danh-muc/loai-ao")
    public ResponseEntity<List<LoaiAo>> danhMucLoaiAo() {
        return ResponseEntity.ok(loaiAoRepo.findAll(Sort.by("idLoaiAo").ascending()));
    }

    @GetMapping({"/danh-muc/phong-cach", "/danh-muc/phong-cach-mac"})
    public ResponseEntity<List<PhongCachMac>> danhMucPhongCach() {
        return ResponseEntity.ok(phongCachMacRepo.findAll(Sort.by("idPhongCachMac").ascending()));
    }

    @GetMapping("/danh-muc/kieu-dang")
    public ResponseEntity<List<KieuDang>> danhMucKieuDang() {
        return ResponseEntity.ok(kieuDangRepo.findAll(Sort.by("idKieuDang").ascending()));
    }

    @GetMapping("/danh-muc/xuat-xu")
    public ResponseEntity<List<XuatXu>> danhMucXuatXu() {
        return ResponseEntity.ok(xuatXuRepo.findAll(Sort.by("idXuatXu").ascending()));
    }

    @GetMapping("/danh-muc/chat-lieu")
    public ResponseEntity<List<ChatLieu>> danhMucChatLieu() {
        return ResponseEntity.ok(chatLieuRepo.findAll(Sort.by("idChatLieu").ascending()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SanPham> getById(@PathVariable Integer id) {
        return sanPhamRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
