package com.example.sp.service.impl;

import com.example.sp.model.customer.KhachHang;
import com.example.sp.repository.KhachHangRepository;
import com.example.sp.service.customer.KhachHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class KhachHangServiceImpl implements KhachHangService {

    private static final String PHONE_PATTERN = "^(03|05|07|08|09)\\d{8}$";
    private static final String CCCD_PATTERN = "^\\d{12}$";

    private final KhachHangRepository khachHangRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Random random = new Random();

    @Override
    public Page<KhachHang> getAll(String keyword, Boolean trangThai, Pageable pageable) {
        String key = isBlank(keyword) ? null : keyword.trim();
        Pageable effectivePageable = withDefaultSort(pageable);
        return khachHangRepository.search(key, trangThai, effectivePageable);
    }

    @Override
    public KhachHang findById(Integer id) {
        return khachHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
    }

    @Override
    @Transactional
    public KhachHang save(KhachHang kh) {
        validate(kh);

        if (kh.getId() == null) {
            kh.setMaKh(generateMaKh());
            kh.setMatKhau(passwordEncoder.encode(generateRandomPassword()));
            if (kh.getTrangThai() == null) {
                kh.setTrangThai(true);
            }
            return khachHangRepository.save(kh);
        }

        KhachHang old = findById(kh.getId());
        old.setTenKhachHang(kh.getTenKhachHang());
        old.setSoDienThoai(kh.getSoDienThoai());
        old.setEmail(kh.getEmail());
        old.setCccd(kh.getCccd());
        old.setGioiTinh(kh.getGioiTinh());
        old.setNgaySinh(kh.getNgaySinh());
        old.setTrangThai(kh.getTrangThai() == null ? old.getTrangThai() : kh.getTrangThai());

        if (!isBlank(kh.getMatKhau())) {
            old.setMatKhau(passwordEncoder.encode(kh.getMatKhau()));
        }

        return khachHangRepository.save(old);
    }

    @Override
    public void delete(Integer id) {
        khachHangRepository.deleteById(id);
    }

    private Pageable withDefaultSort(Pageable pageable) {
        Sort sort = Sort.by(Sort.Order.desc("trangThai"), Sort.Order.desc("id"));
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged(sort);
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private void validate(KhachHang kh) {
        if (isBlank(kh.getTenKhachHang())) {
            throw new RuntimeException("Tên khách hàng không được để trống");
        }

        Integer id = kh.getId() == null ? 0 : kh.getId();

        if (!isBlank(kh.getEmail())) {
            kh.setEmail(kh.getEmail().trim());
            if (!kh.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new RuntimeException("Email không hợp lệ");
            }
            if (khachHangRepository.existsByEmailAndIdNot(kh.getEmail(), id)) {
                throw new RuntimeException("Email đã được sử dụng");
            }
        }

        if (!isBlank(kh.getSoDienThoai())) {
            kh.setSoDienThoai(kh.getSoDienThoai().trim());
            if (!kh.getSoDienThoai().matches(PHONE_PATTERN)) {
                throw new RuntimeException("Số điện thoại không hợp lệ");
            }
            if (khachHangRepository.existsBySoDienThoaiAndIdNot(kh.getSoDienThoai(), id)) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }
        }

        if (!isBlank(kh.getCccd())) {
            kh.setCccd(kh.getCccd().trim());
            if (!kh.getCccd().matches(CCCD_PATTERN)) {
                throw new RuntimeException("CCCD phải có đúng 12 chữ số");
            }
            if (khachHangRepository.existsByCccdAndIdNot(kh.getCccd(), id)) {
                throw new RuntimeException("CCCD đã tồn tại");
            }
        }
    }

    private String generateMaKh() {
        String maxMaKh = khachHangRepository.findMaxMaKh();
        if (maxMaKh == null || maxMaKh.trim().isEmpty()) {
            return "KH001";
        }
        try {
            String numberPart = maxMaKh.substring(2);
            int currentNumber = Integer.parseInt(numberPart);
            return String.format("KH%03d", currentNumber + 1);
        } catch (Exception e) {
            return "KH001";
        }
    }

    private String generateRandomPassword() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}