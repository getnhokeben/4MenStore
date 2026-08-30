package com.example.sp.controller.sanpham;

import java.text.Normalizer;
import com.example.sp.model.sanpham.ChatLieu;
import com.example.sp.model.sanpham.KichCo;
import com.example.sp.model.sanpham.KieuDang;
import com.example.sp.model.sanpham.LoaiAo;
import com.example.sp.model.sanpham.MauSac;
import com.example.sp.model.sanpham.PhongCachMac;
import com.example.sp.model.sanpham.XuatXu;
import com.example.sp.repository.sanpham.ChatLieuRepository;
import com.example.sp.repository.sanpham.KichCoRepository;
import com.example.sp.repository.sanpham.KieuDangRepository;
import com.example.sp.repository.sanpham.LoaiAoRepository;
import com.example.sp.repository.sanpham.MauSacRepository;
import com.example.sp.repository.sanpham.PhongCachMacRepository;
import com.example.sp.repository.sanpham.XuatXuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@RestController
public class ThuocTinhController {
    private static final int MIN_CODE_LENGTH = 2;
    private static final int MAX_CODE_LENGTH = 20;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;
    private static final Set<String> ALLOWED_SIZE_NAMES = Set.of("XS", "S", "M", "L", "XL", "XXL", "XXXL");

    @Autowired private KichCoRepository kichCoRepo;
    @Autowired private MauSacRepository mauSacRepo;
    @Autowired private LoaiAoRepository loaiAoRepo;
    @Autowired private PhongCachMacRepository phongCachMacRepo;
    @Autowired private KieuDangRepository kieuDangRepo;
    @Autowired private XuatXuRepository xuatXuRepo;
    @Autowired private ChatLieuRepository chatLieuRepo;

    // Thực hiện xử lý nghiệp vụ của hàm attribute config.
    private record AttributeConfig(
            Supplier<List<Object>> findAllSupplier,
            Function<Integer, Optional<Object>> findByIdFn,
            Function<Object, Object> saveFn,
            Function<Integer, Boolean> existsByIdFn,
            VoidConsumer<Integer> deleteByIdFn,
            Supplier<Object> creator,
            Function<Object, Integer> idGetter,
            Function<Object, String> codeGetter,
            Function<Object, String> nameGetter,
            Function<Object, Boolean> statusGetter,
            BiConsumer<Object, String> codeSetter,
            BiConsumer<Object, String> nameSetter,
            BiConsumer<Object, Boolean> statusSetter
    ) {
        interface VoidConsumer<T> {
            void accept(T t);
        }
    }

    // --- HTML page ---
    @GetMapping(value = "/thuoc-tinh/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    // Thực hiện xử lý nghiệp vụ của hàm page.
    public ResponseEntity<byte[]> page(@PathVariable String slug) throws IOException {
        if (config(slug) == null) return ResponseEntity.notFound().build();

        byte[] html = new ClassPathResource("templates/ThuocTinh/ThuocTinh.html")
                .getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    // --- LIST with filter + pagination ---
    @GetMapping("/api/thuoc-tinh/{slug}")
    // Tải hoặc truy xuất dữ liệu cho list.
    public ResponseEntity<?> list(@PathVariable String slug,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Boolean trangThai) {
        AttributeConfig cfg = config(slug);
        if (cfg == null) return ResponseEntity.notFound().build();

        int pageIndex = Math.max(0, page);
        int pageSize = Math.max(1, Math.min(size, 9999));
        String normalizedKeyword = normalize(keyword);

        List<Object> filtered = cfg.findAllSupplier().get().stream()
                .filter(item -> {
                    if (normalizedKeyword == null) return true;
                    String code = normalize(cfg.codeGetter.apply(item));
                    String name = normalize(cfg.nameGetter.apply(item));
                    return (code != null && code.contains(normalizedKeyword))
                            || (name != null && name.contains(normalizedKeyword));
                })
                .filter(item -> {
                    if (trangThai == null) return true;
                    Boolean status = cfg.statusGetter.apply(item);
                    return trangThai.equals(status == null ? Boolean.TRUE : status);
                })
                .sorted(Comparator.comparing((Object item) -> cfg.idGetter.apply(item),
                        Comparator.nullsLast(Integer::compareTo)).reversed())
                .toList();

        int from = Math.min(pageIndex * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        return ResponseEntity.ok(new PageImpl<>(filtered.subList(from, to), pageable, filtered.size()));
    }

    // --- GET by ID ---
    @GetMapping("/api/thuoc-tinh/{slug}/{id}")
    // Tải hoặc truy xuất dữ liệu cho get by id.
    public ResponseEntity<?> getById(@PathVariable String slug, @PathVariable Integer id) {
        AttributeConfig cfg = config(slug);
        if (cfg == null) return ResponseEntity.notFound().build();
        return cfg.findByIdFn.apply(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- CREATE ---
    @PostMapping("/api/thuoc-tinh/{slug}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create.
    public ResponseEntity<?> create(@PathVariable String slug, @RequestBody Map<String, Object> body) {
        AttributeConfig cfg = config(slug);
        if (cfg == null) return ResponseEntity.notFound().build();
        try {
            Object entity = cfg.creator.get();
            applyBody(slug, cfg, entity, body, true, null);
            Object saved = cfg.saveFn.apply(entity);
            return ResponseEntity.status(201).body(saved);
        } catch (DataIntegrityViolationException e) {
            return badJson("Mã thuộc tính đã tồn tại, vui lòng nhập mã khác");
        } catch (DataAccessException e) {
            return badJson("Không thể lưu thuộc tính. Vui lòng kiểm tra lại mã, tên và độ dài dữ liệu");
        } catch (IllegalArgumentException e) {
            return badJson(e.getMessage());
        }
    }

    // --- UPDATE ---
    @PutMapping("/api/thuoc-tinh/{slug}/{id}")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update.
    public ResponseEntity<?> update(@PathVariable String slug,
                                    @PathVariable Integer id,
                                    @RequestBody Map<String, Object> body) {
        AttributeConfig cfg = config(slug);
        if (cfg == null) return ResponseEntity.notFound().build();
        try {
            Optional<Object> opt = cfg.findByIdFn.apply(id);
            if (opt.isEmpty()) return notFoundJson("Không tìm thấy thuộc tính với id: " + id);

            Object entity = opt.get();
            applyBody(slug, cfg, entity, body, false, id);
            Object saved = cfg.saveFn.apply(entity);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            return badJson("Mã thuộc tính đã tồn tại, vui lòng nhập mã khác");
        } catch (DataAccessException e) {
            return badJson("Không thể lưu thuộc tính. Vui lòng kiểm tra lại mã, tên và độ dài dữ liệu");
        } catch (IllegalArgumentException e) {
            return badJson(e.getMessage());
        }
    }

    // --- TOGGLE STATUS ---
    @PatchMapping("/api/thuoc-tinh/{slug}/{id}/trang-thai")
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update status.
    public ResponseEntity<?> updateStatus(@PathVariable String slug,
                                          @PathVariable Integer id,
                                          @RequestBody Map<String, Object> body) {
        AttributeConfig cfg = config(slug);
        if (cfg == null) return ResponseEntity.notFound().build();
        try {
            if (body == null || !body.containsKey("trangThai") || body.get("trangThai") == null) {
                return badJson("Thiếu trường trangThai");
            }
            Optional<Object> opt = cfg.findByIdFn.apply(id);
            if (opt.isEmpty()) return notFoundJson("Không tìm thấy thuộc tính với id: " + id);

            Object entity = opt.get();
            boolean status = Boolean.parseBoolean(String.valueOf(body.get("trangThai")));
            cfg.statusSetter.accept(entity, status);
            Object saved = cfg.saveFn.apply(entity);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            return badJson("Không thể cập nhật trạng thái");
        }
    }

    // --- DELETE ---
    @DeleteMapping("/api/thuoc-tinh/{slug}/{id}")
    // Xử lý thao tác đóng, xóa hoặc hủy cho delete.
    public ResponseEntity<?> delete(@PathVariable String slug, @PathVariable Integer id) {
        AttributeConfig cfg = config(slug);
        if (cfg == null) return ResponseEntity.notFound().build();
        try {
            if (!cfg.existsByIdFn.apply(id)) return notFoundJson("Không tìm thấy thuộc tính với id: " + id);
            cfg.deleteByIdFn.accept(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return badJson("Không thể xóa vì thuộc tính đang được sử dụng bởi sản phẩm");
        }
    }

    // --- Helpers ---

    // Thực hiện xử lý nghiệp vụ của hàm apply body.
    private void applyBody(String slug, AttributeConfig cfg, Object entity, Map<String, Object> body, boolean creating, Integer currentId) {
        String code = textValue(body, "ma");
        String name = cleanAttributeName(textValue(body, "ten"));
        List<Object> all = cfg.findAllSupplier().get();
        if ((code == null || code.isBlank()) && creating) {
            code = generateUniqueCode(slug, cfg, all);
        }
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mã thuộc tính");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Vui lòng nhập tên thuộc tính");
        if ("kich-co".equals(slug)) {
            name = normalizeSizeName(name);
        }
        if (code.length() < MIN_CODE_LENGTH || code.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("Mã thuộc tính phải từ " + MIN_CODE_LENGTH + " đến " + MAX_CODE_LENGTH + " ký tự");
        }
        if (!code.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Mã thuộc tính chỉ được chứa chữ, số, gạch ngang hoặc gạch dưới");
        }
        int minNameLength = "kich-co".equals(slug) ? 1 : MIN_NAME_LENGTH;
        if (name.length() < minNameLength || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Tên thuộc tính phải từ " + minNameLength + " đến " + MAX_NAME_LENGTH + " ký tự");
        }

        // Tên được so sánh sau khi chuẩn hóa: bỏ khoảng trắng thừa và không phân biệt hoa/thường.
        String newNameKey = attributeNameKey(name);

        for (Object item : all) {
            Integer itemId = cfg.idGetter.apply(item);
            if (currentId != null && currentId.equals(itemId)) {
                continue;
            }

            String itemCode = cfg.codeGetter.apply(item);
            if (itemCode != null && itemCode.trim().equalsIgnoreCase(code)) {
                throw new IllegalArgumentException("Mã thuộc tính '" + code + "' đã tồn tại, vui lòng nhập mã khác");
            }

            String itemName = cfg.nameGetter.apply(item);
            String itemNameKey = attributeNameKey(itemName);
            if (itemNameKey != null && itemNameKey.equals(newNameKey)) {
                throw new IllegalArgumentException("Tên thuộc tính '" + name + "' đã tồn tại, vui lòng nhập tên khác");
            }
        }

        cfg.codeSetter.accept(entity, code);
        cfg.nameSetter.accept(entity, name);

        if (creating) {
            cfg.statusSetter.accept(entity, true);
        } else if (body.containsKey("trangThai") && body.get("trangThai") != null) {
            cfg.statusSetter.accept(entity, Boolean.parseBoolean(String.valueOf(body.get("trangThai"))));
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm generate unique code.
    private String generateUniqueCode(String slug, AttributeConfig cfg, List<Object> all) {
        String prefix = codePrefix(slug);
        for (int i = 0; i < 50; i++) {
            String millis = String.valueOf(System.currentTimeMillis());
            String suffix = millis.substring(Math.max(0, millis.length() - 8))
                    + ThreadLocalRandom.current().nextInt(10, 100);
            String code = prefix + suffix;
            boolean exists = all.stream().anyMatch(item -> {
                String itemCode = cfg.codeGetter.apply(item);
                return itemCode != null && itemCode.trim().equalsIgnoreCase(code);
            });
            if (!exists) return code;
        }
        String fallback = prefix + Long.toString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
        return fallback.length() > MAX_CODE_LENGTH ? fallback.substring(0, MAX_CODE_LENGTH) : fallback;
    }

    // Thực hiện xử lý nghiệp vụ của hàm code prefix.
    private String codePrefix(String slug) {
        if (slug == null || slug.isBlank()) return "TT";
        StringBuilder prefix = new StringBuilder();
        for (String part : slug.split("-")) {
            if (!part.isBlank()) prefix.append(Character.toUpperCase(part.charAt(0)));
        }
        return prefix.isEmpty() ? "TT" : prefix.toString();
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize size name.
    private String normalizeSizeName(String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_SIZE_NAMES.contains(normalized)) {
            throw new IllegalArgumentException("Kích cỡ chỉ được chọn: XS, S, M, L, XL, XXL, XXXL");
        }
        return normalized;
    }

    // Thực hiện xử lý nghiệp vụ của hàm text value.
    private String textValue(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) return null;
        String value = String.valueOf(body.get(key)).trim();
        return value.isEmpty() ? null : value;
    }
    // Thực hiện xử lý nghiệp vụ của hàm clean attribute name.
    private String cleanAttributeName(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = Normalizer
                .normalize(value, Normalizer.Form.NFC)
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned.isEmpty() ? null : cleaned;
    }

    // Thực hiện xử lý nghiệp vụ của hàm attribute name key.
    private String attributeNameKey(String value) {
        String cleaned = cleanAttributeName(value);

        if (cleaned == null) {
            return null;
        }

        return cleaned.toLowerCase(Locale.ROOT);
    }
    /** Returns a 400 response with a consistent JSON body {"message": "..."} */
    // Thực hiện xử lý nghiệp vụ của hàm bad json.
    private ResponseEntity<Map<String, String>> badJson(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }

    /** Returns a 404 response with a consistent JSON body {"message": "..."} */
    // Thực hiện xử lý nghiệp vụ của hàm not found json.
    private ResponseEntity<Map<String, String>> notFoundJson(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", message);
        return ResponseEntity.status(404).body(body);
    }

    @SuppressWarnings("unchecked")
    // Thực hiện xử lý nghiệp vụ của hàm config.
    private AttributeConfig config(String slug) {
        return switch (slug) {
            case "kich-co" -> new AttributeConfig(
                    () -> (List<Object>) (List<?>) kichCoRepo.findAll(),
                    id -> kichCoRepo.findById(id).map(Object.class::cast),
                    entity -> kichCoRepo.save((KichCo) entity),
                    id -> kichCoRepo.existsById(id),
                    id -> kichCoRepo.deleteById(id),
                    KichCo::new,
                    item -> ((KichCo) item).getIdKichCo(),
                    item -> ((KichCo) item).getMaKichCo(),
                    item -> ((KichCo) item).getTenKichCo(),
                    item -> ((KichCo) item).getTrangThai(),
                    (item, value) -> ((KichCo) item).setMaKichCo(value),
                    (item, value) -> ((KichCo) item).setTenKichCo(value),
                    (item, value) -> ((KichCo) item).setTrangThai(value));
            case "mau-sac" -> new AttributeConfig(
                    () -> (List<Object>) (List<?>) mauSacRepo.findAll(),
                    id -> mauSacRepo.findById(id).map(Object.class::cast),
                    entity -> mauSacRepo.save((MauSac) entity),
                    id -> mauSacRepo.existsById(id),
                    id -> mauSacRepo.deleteById(id),
                    MauSac::new,
                    item -> ((MauSac) item).getIdMauSac(),
                    item -> ((MauSac) item).getMaMauSac(),
                    item -> ((MauSac) item).getTenMauSac(),
                    item -> ((MauSac) item).getTrangThai(),
                    (item, value) -> ((MauSac) item).setMaMauSac(value),
                    (item, value) -> ((MauSac) item).setTenMauSac(value),
                    (item, value) -> ((MauSac) item).setTrangThai(value));
            case "loai-ao" -> new AttributeConfig(
                    () -> (List<Object>) (List<?>) loaiAoRepo.findAll(),
                    id -> loaiAoRepo.findById(id).map(Object.class::cast),
                    entity -> loaiAoRepo.save((LoaiAo) entity),
                    id -> loaiAoRepo.existsById(id),
                    id -> loaiAoRepo.deleteById(id),
                    LoaiAo::new,
                    item -> ((LoaiAo) item).getIdLoaiAo(),
                    item -> ((LoaiAo) item).getMaLoai(),
                    item -> ((LoaiAo) item).getTenLoai(),
                    item -> ((LoaiAo) item).getTrangThai(),
                    (item, value) -> ((LoaiAo) item).setMaLoai(value),
                    (item, value) -> ((LoaiAo) item).setTenLoai(value),
                    (item, value) -> ((LoaiAo) item).setTrangThai(value));
            case "phong-cach-mac" -> new AttributeConfig(
                    () -> (List<Object>) (List<?>) phongCachMacRepo.findAll(),
                    id -> phongCachMacRepo.findById(id).map(Object.class::cast),
                    entity -> phongCachMacRepo.save((PhongCachMac) entity),
                    id -> phongCachMacRepo.existsById(id),
                    id -> phongCachMacRepo.deleteById(id),
                    PhongCachMac::new,
                    item -> ((PhongCachMac) item).getIdPhongCachMac(),
                    item -> ((PhongCachMac) item).getMaPhongCachMac(),
                    item -> ((PhongCachMac) item).getTenPhongCach(),
                    item -> ((PhongCachMac) item).getTrangThai(),
                    (item, value) -> ((PhongCachMac) item).setMaPhongCachMac(value),
                    (item, value) -> ((PhongCachMac) item).setTenPhongCach(value),
                    (item, value) -> ((PhongCachMac) item).setTrangThai(value));
            case "kieu-dang" -> new AttributeConfig(
                    () -> (List<Object>) (List<?>) kieuDangRepo.findAll(),
                    id -> kieuDangRepo.findById(id).map(Object.class::cast),
                    entity -> kieuDangRepo.save((KieuDang) entity),
                    id -> kieuDangRepo.existsById(id),
                    id -> kieuDangRepo.deleteById(id),
                    KieuDang::new,
                    item -> ((KieuDang) item).getIdKieuDang(),
                    item -> ((KieuDang) item).getMaKieuDang(),
                    item -> ((KieuDang) item).getTenKieuDang(),
                    item -> ((KieuDang) item).getTrangThai(),
                    (item, value) -> ((KieuDang) item).setMaKieuDang(value),
                    (item, value) -> ((KieuDang) item).setTenKieuDang(value),
                    (item, value) -> ((KieuDang) item).setTrangThai(value));
            case "xuat-xu" -> new AttributeConfig(
                    () -> (List<Object>) (List<?>) xuatXuRepo.findAll(),
                    id -> xuatXuRepo.findById(id).map(Object.class::cast),
                    entity -> xuatXuRepo.save((XuatXu) entity),
                    id -> xuatXuRepo.existsById(id),
                    id -> xuatXuRepo.deleteById(id),
                    XuatXu::new,
                    item -> ((XuatXu) item).getIdXuatXu(),
                    item -> ((XuatXu) item).getMaXuatXu(),
                    item -> ((XuatXu) item).getTenXuatXu(),
                    item -> ((XuatXu) item).getTrangThai(),
                    (item, value) -> ((XuatXu) item).setMaXuatXu(value),
                    (item, value) -> ((XuatXu) item).setTenXuatXu(value),
                    (item, value) -> ((XuatXu) item).setTrangThai(value));
            case "chat-lieu" -> new AttributeConfig(
                    () -> (List<Object>) (List<?>) chatLieuRepo.findAll(),
                    id -> chatLieuRepo.findById(id).map(Object.class::cast),
                    entity -> chatLieuRepo.save((ChatLieu) entity),
                    id -> chatLieuRepo.existsById(id),
                    id -> chatLieuRepo.deleteById(id),
                    ChatLieu::new,
                    item -> ((ChatLieu) item).getIdChatLieu(),
                    item -> ((ChatLieu) item).getMaChatLieu(),
                    item -> ((ChatLieu) item).getTenChatLieu(),
                    item -> ((ChatLieu) item).getTrangThai(),
                    (item, value) -> ((ChatLieu) item).setMaChatLieu(value),
                    (item, value) -> ((ChatLieu) item).setTenChatLieu(value),
                    (item, value) -> ((ChatLieu) item).setTrangThai(value));
            default -> null;
        };
    }
    // Thực hiện xử lý nghiệp vụ của hàm normalize.
    private String normalize(String value) {
        String cleaned = cleanAttributeName(value);

        return cleaned == null
                ? null
                : cleaned.toLowerCase(Locale.ROOT);
    }
}
