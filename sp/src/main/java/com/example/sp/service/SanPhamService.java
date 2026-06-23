package com.example.sp.service;

import com.example.sp.dto.ChiTietSanPhamRequest;
import com.example.sp.dto.ChiTietSanPhamUpdateRequest;
import com.example.sp.dto.SanPhamFullRequest;
import com.example.sp.model.ChatLieu;
import com.example.sp.model.ChiTietSanPham;
import com.example.sp.model.HinhAnhSanPham;
import com.example.sp.model.KichCo;
import com.example.sp.model.KieuDang;
import com.example.sp.model.LoaiAo;
import com.example.sp.model.MauSac;
import com.example.sp.model.PhongCachMac;
import com.example.sp.model.SanPham;
import com.example.sp.model.XuatXu;
import com.example.sp.repository.ChatLieuRepository;
import com.example.sp.repository.ChiTietSanPhamRepository;
import com.example.sp.repository.HinhAnhSanPhamRepository;
import com.example.sp.repository.KichCoRepository;
import com.example.sp.repository.KieuDangRepository;
import com.example.sp.repository.LoaiAoRepository;
import com.example.sp.repository.MauSacRepository;
import com.example.sp.repository.PhongCachMacRepository;
import com.example.sp.repository.SanPhamRepository;
import com.example.sp.repository.XuatXuRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class SanPhamService {

    @Autowired private SanPhamRepository sanPhamRepo;
    @Autowired private ChiTietSanPhamRepository chiTietRepo;
    @Autowired private HinhAnhSanPhamRepository hinhAnhRepo;
    @Autowired private XuatXuRepository xuatXuRepo;
    @Autowired private ChatLieuRepository chatLieuRepo;
    @Autowired private LoaiAoRepository loaiAoRepo;
    @Autowired private KichCoRepository kichCoRepo;
    @Autowired private MauSacRepository mauSacRepo;
    @Autowired private PhongCachMacRepository phongCachMacRepo;
    @Autowired private KieuDangRepository kieuDangRepo;
    @Autowired private EntityManager entityManager;

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) throw new RuntimeException(message);
        return trimmed;
    }

    private List<ChiTietSanPhamRequest> variantsOf(SanPhamFullRequest request) {
        return request.getDanhSachBienThe() == null
                ? Collections.emptyList()
                : request.getDanhSachBienThe();
    }

    private void validateProductRequest(SanPhamFullRequest request, boolean creating) {
        if (request == null) throw new RuntimeException("Dữ liệu sản phẩm không hợp lệ");
        if (creating) requireText(request.getMaSp(), "Mã sản phẩm không được để trống");
        requireText(request.getTenSp(), "Tên sản phẩm không được để trống");
    }

    private void validatePrices(BigDecimal donGia) {
        if (donGia != null && donGia.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Đơn giá không được nhỏ hơn 0");
        }
    }

    private String validateSku(String sku, Integer currentId) {
        String normalized = requireText(sku, "Mã chi tiết sản phẩm không được trống");
        chiTietRepo.findByMaChiTietSanPham(normalized)
                .filter(existing -> currentId == null || !Objects.equals(existing.getIdSpct(), currentId))
                .ifPresent(existing -> {
                    throw new RuntimeException("Mã SKU '" + normalized + "' đã tồn tại");
                });
        return normalized;
    }

    private XuatXu requireXuatXu(Integer id) {
        if (id == null) return null;
        if (!xuatXuRepo.existsById(id)) throw new RuntimeException("Xuất xứ id=" + id + " không tồn tại");
        return entityManager.getReference(XuatXu.class, id);
    }

    private ChatLieu requireChatLieu(Integer id) {
        if (id == null) return null;
        if (!chatLieuRepo.existsById(id)) throw new RuntimeException("Chất liệu id=" + id + " không tồn tại");
        return entityManager.getReference(ChatLieu.class, id);
    }

    private KichCo requireKichCo(Integer id) {
        if (id == null) throw new RuntimeException("Vui lòng chọn kích cỡ");
        if (!kichCoRepo.existsById(id)) throw new RuntimeException("Kích cỡ id=" + id + " không tồn tại");
        return entityManager.getReference(KichCo.class, id);
    }

    private MauSac requireMauSac(Integer id) {
        if (id == null) throw new RuntimeException("Vui lòng chọn màu sắc");
        if (!mauSacRepo.existsById(id)) throw new RuntimeException("Màu sắc id=" + id + " không tồn tại");
        return entityManager.getReference(MauSac.class, id);
    }

    private LoaiAo requireLoaiAo(Integer id) {
        if (id == null) throw new RuntimeException("Vui lòng chọn loại áo");
        if (!loaiAoRepo.existsById(id)) throw new RuntimeException("Loại áo id=" + id + " không tồn tại");
        return entityManager.getReference(LoaiAo.class, id);
    }

    private PhongCachMac requirePhongCachMac(Integer id) {
        if (id == null) throw new RuntimeException("Vui lòng chọn phong cách");
        if (!phongCachMacRepo.existsById(id)) throw new RuntimeException("Phong cách id=" + id + " không tồn tại");
        return entityManager.getReference(PhongCachMac.class, id);
    }

    private KieuDang requireKieuDang(Integer id) {
        if (id == null) throw new RuntimeException("Vui lòng chọn kiểu dáng");
        if (!kieuDangRepo.existsById(id)) throw new RuntimeException("Kiểu dáng id=" + id + " không tồn tại");
        return entityManager.getReference(KieuDang.class, id);
    }

    private XuatXu findOrCreateXuatXu(String tenXuatXu) {
        String normalized = trimToNull(tenXuatXu);
        if (normalized == null) return null;

        List<XuatXu> existing = xuatXuRepo.findByTenXuatXu(normalized);
        if (!existing.isEmpty()) return existing.get(0);

        XuatXu newXx = new XuatXu();
        newXx.setMaXuatXu("XX_" + System.currentTimeMillis());
        newXx.setTenXuatXu(normalized);
        return xuatXuRepo.save(newXx);
    }

    private ChatLieu findOrCreateChatLieu(String tenChatLieu) {
        String normalized = trimToNull(tenChatLieu);
        if (normalized == null) return null;

        List<ChatLieu> existing = chatLieuRepo.findByTenChatLieu(normalized);
        if (!existing.isEmpty()) return existing.get(0);

        ChatLieu newCl = new ChatLieu();
        newCl.setMaChatLieu("CL_" + System.currentTimeMillis());
        newCl.setTenChatLieu(normalized);
        return chatLieuRepo.save(newCl);
    }

    private void applyProductFields(SanPham sp, SanPhamFullRequest request, boolean creating) {
        sp.setTenSp(requireText(request.getTenSp(), "Tên sản phẩm không được để trống"));
        sp.setMoTa(request.getMoTa());
        if (request.getHinhAnhChinh() != null) {
            sp.setHinhAnh(request.getHinhAnhChinh());
        }

        String tenXuatXu = trimToNull(request.getTenXuatXu());
        if (tenXuatXu != null) {
            sp.setXuatXu(findOrCreateXuatXu(tenXuatXu));
        } else if (request.getIdXuatXu() != null) {
            sp.setXuatXu(requireXuatXu(request.getIdXuatXu()));
        }

        String tenChatLieu = trimToNull(request.getTenChatLieu());
        if (tenChatLieu != null) {
            sp.setChatLieu(findOrCreateChatLieu(tenChatLieu));
        } else if (request.getIdChatLieu() != null) {
            sp.setChatLieu(requireChatLieu(request.getIdChatLieu()));
        }

        if (creating) {
            sp.setNguoiTao(request.getNguoiThucHien());
        } else {
            sp.setNgayCapNhat(LocalDateTime.now());
            sp.setNguoiCapNhat(request.getNguoiThucHien());
        }
    }

    private void saveSubImages(Integer idSp, List<String> urls) {
        if (urls == null) return;
        for (String url : urls) {
            String normalized = trimToNull(url);
            if (normalized == null) continue;
            HinhAnhSanPham ha = new HinhAnhSanPham();
            ha.setIdSanPham(idSp);
            ha.setUrlAnh(normalized);
            hinhAnhRepo.save(ha);
        }
    }

    private void replaceSubImages(Integer idSp, List<String> urls) {
        if (urls == null) return;
        hinhAnhRepo.deleteByIdSanPham(idSp);
        saveSubImages(idSp, urls);
    }

    private void applyVariantFields(ChiTietSanPham ct, ChiTietSanPhamRequest ctDto, String nguoiThucHien, boolean creating) {
        if (ctDto == null) throw new RuntimeException("Dữ liệu biến thể không hợp lệ");

        String sku = validateSku(ctDto.getMaChiTietSanPham(), ctDto.getIdSpct());
        validatePrices(ctDto.getDonGia());
        if (ctDto.getSoLuongTon() == null) throw new RuntimeException("Số lượng không được trống");
        if (ctDto.getSoLuongTon() < 0) throw new RuntimeException("Số lượng tồn kho không được âm");

        ct.setMaChiTietSanPham(sku);
        ct.setSoLuongTon(ctDto.getSoLuongTon());
        ct.setDonGia(ctDto.getDonGia());
        ct.setKichCo(requireKichCo(ctDto.getIdKichCo()));
        ct.setMauSac(requireMauSac(ctDto.getIdMauSac()));
        ct.setLoaiAo(requireLoaiAo(ctDto.getIdLoaiAo()));
        ct.setPhongCachMac(requirePhongCachMac(ctDto.getIdPhongCachMac()));
        ct.setKieuDang(requireKieuDang(ctDto.getIdKieuDang()));

        if (creating) {
            ct.setTrangThai(true);
            ct.setNguoiTao(nguoiThucHien);
        } else {
            ct.setNgayCapNhat(LocalDateTime.now());
            ct.setNguoiCapNhat(nguoiThucHien);
        }
    }

    @Transactional
    public SanPham createProduct(SanPhamFullRequest request) {
        validateProductRequest(request, true);
        List<ChiTietSanPhamRequest> variants = variantsOf(request);
        if (variants.isEmpty()) throw new RuntimeException("Sản phẩm phải có ít nhất một biến thể chi tiết");

        String maSp = requireText(request.getMaSp(), "Mã sản phẩm không được để trống");
        if (sanPhamRepo.existsByMaSp(maSp)) {
            throw new RuntimeException("Mã sản phẩm '" + maSp + "' đã tồn tại");
        }

        SanPham sp = new SanPham();
        sp.setMaSp(maSp);
        applyProductFields(sp, request, true);
        sp = sanPhamRepo.save(sp);

        saveSubImages(sp.getIdSp(), request.getDanhSachHinhAnhPhu());

        for (ChiTietSanPhamRequest ctDto : variants) {
            ChiTietSanPham ct = new ChiTietSanPham();
            ct.setIdSanPham(sp.getIdSp());
            applyVariantFields(ct, ctDto, request.getNguoiThucHien(), true);
            chiTietRepo.save(ct);
        }
        return sp;
    }

    public Page<SanPham> getProducts(String keyword, String chatLieu, Boolean trangThai, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("idSp").descending());
        return sanPhamRepo.findByFilters(trimToNull(keyword), trimToNull(chatLieu), trangThai, pageable);
    }

    public List<ChiTietSanPham> getProductVariants(Integer idSp) {
        return chiTietRepo.findByIdSanPham(idSp);
    }

    @Transactional
    public SanPham updateProduct(Integer idSp, SanPhamFullRequest request) {
        validateProductRequest(request, false);

        SanPham sp = sanPhamRepo.findById(idSp)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        applyProductFields(sp, request, false);
        sp = sanPhamRepo.save(sp);
        replaceSubImages(idSp, request.getDanhSachHinhAnhPhu());

        List<ChiTietSanPhamRequest> requestedVariants = variantsOf(request);
        if (requestedVariants.isEmpty()) return sp;

        List<ChiTietSanPham> oldVariants = chiTietRepo.findByIdSanPham(idSp);
        List<Integer> keepIds = requestedVariants.stream()
                .map(ChiTietSanPhamRequest::getIdSpct)
                .filter(Objects::nonNull)
                .toList();

        oldVariants.stream()
                .filter(ct -> !keepIds.contains(ct.getIdSpct()))
                .forEach(ct -> {
                    ct.setTrangThai(false);
                    ct.setNgayCapNhat(LocalDateTime.now());
                    ct.setNguoiCapNhat(request.getNguoiThucHien());
                    chiTietRepo.save(ct);
                });

        for (ChiTietSanPhamRequest ctDto : requestedVariants) {
            if (ctDto.getIdSpct() != null) {
                ChiTietSanPham ct = chiTietRepo.findById(ctDto.getIdSpct())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể id=" + ctDto.getIdSpct()));
                if (!Objects.equals(ct.getIdSanPham(), idSp)) {
                    throw new RuntimeException("Biến thể id=" + ctDto.getIdSpct() + " không thuộc sản phẩm này");
                }
                applyVariantFields(ct, ctDto, request.getNguoiThucHien(), false);
                chiTietRepo.save(ct);
            } else {
                ChiTietSanPham ct = new ChiTietSanPham();
                ct.setIdSanPham(idSp);
                applyVariantFields(ct, ctDto, request.getNguoiThucHien(), true);
                chiTietRepo.save(ct);
            }
        }

        return sp;
    }

    @Transactional
    public ChiTietSanPham updateVariant(Integer idSpct, ChiTietSanPhamUpdateRequest request) {
        if (request == null) throw new RuntimeException("Dữ liệu biến thể không hợp lệ");

        ChiTietSanPham ct = chiTietRepo.findById(idSpct)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể id=" + idSpct));

        BigDecimal donGia = request.getDonGia() != null ? request.getDonGia() : ct.getDonGia();
        validatePrices(donGia);

        String sku = trimToNull(request.getMaChiTietSanPham());
        if (sku != null) ct.setMaChiTietSanPham(validateSku(sku, idSpct));
        if (request.getIdKichCo() != null) ct.setKichCo(requireKichCo(request.getIdKichCo()));
        if (request.getIdMauSac() != null) ct.setMauSac(requireMauSac(request.getIdMauSac()));
        if (request.getIdLoaiAo() != null) ct.setLoaiAo(requireLoaiAo(request.getIdLoaiAo()));
        if (request.getIdPhongCachMac() != null) ct.setPhongCachMac(requirePhongCachMac(request.getIdPhongCachMac()));
        if (request.getIdKieuDang() != null) ct.setKieuDang(requireKieuDang(request.getIdKieuDang()));

        if (request.getSoLuongTon() != null) {
            if (request.getSoLuongTon() < 0) throw new RuntimeException("Số lượng tồn kho không được âm");
            ct.setSoLuongTon(request.getSoLuongTon());
        }
        if (request.getDonGia() != null) ct.setDonGia(request.getDonGia());
        if (request.getTrangThai() != null) ct.setTrangThai(request.getTrangThai());

        ct.setNgayCapNhat(LocalDateTime.now());
        ct.setNguoiCapNhat(trimToNull(request.getNguoiThucHien()) == null ? "Admin" : request.getNguoiThucHien().trim());
        return chiTietRepo.save(ct);
    }

    @Transactional
    public void softDeleteProduct(Integer idSp) {
        SanPham sp = sanPhamRepo.findById(idSp)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        sp.setTrangThai(false);
        sp.setNgayCapNhat(LocalDateTime.now());
        sanPhamRepo.save(sp);

        List<ChiTietSanPham> variants = chiTietRepo.findByIdSanPham(idSp);
        variants.forEach(ct -> {
            ct.setTrangThai(false);
            ct.setNgayCapNhat(LocalDateTime.now());
        });
        chiTietRepo.saveAll(variants);
    }

    @Transactional
    public void setProductStatus(Integer idSp, boolean status) {
        SanPham sp = sanPhamRepo.findById(idSp)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        sp.setTrangThai(status);
        sp.setNgayCapNhat(LocalDateTime.now());
        sanPhamRepo.save(sp);

        List<ChiTietSanPham> variants = chiTietRepo.findByIdSanPham(idSp);
        variants.forEach(ct -> {
            ct.setTrangThai(status);
            ct.setNgayCapNhat(LocalDateTime.now());
        });
        chiTietRepo.saveAll(variants);
    }
}
