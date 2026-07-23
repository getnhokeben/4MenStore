package com.example.sp.service.khachhang;

import com.example.sp.dto.khachhang.DiaChiKhachHangDTO;
import com.example.sp.dto.khachhang.DiaChiKhachHangRequest;
import com.example.sp.model.khachhang.DiaChi;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.repository.khachhang.DiaChiRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DiaChiKhachHangService {

    private static final DateTimeFormatter CODE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final DiaChiRepository diaChiRepository;
    private final KhachHangRepository khachHangRepository;

    @Transactional(readOnly = true)
    public List<DiaChiKhachHangDTO> findByCustomer(Integer customerId) {
        ensureCustomer(customerId);

        return diaChiRepository
                .findByKhachHang_IdOrderByMacDinhDescIdAsc(customerId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DiaChiKhachHangDTO create(
            Integer customerId,
            DiaChiKhachHangRequest request
    ) {
        KhachHang customer = ensureCustomer(customerId);

        boolean firstAddress =
                diaChiRepository.countByKhachHang_Id(customerId) == 0;

        DiaChi address = DiaChi.builder()
                .khachHang(customer)
                .maDiaChi(generateCode())
                .trangThai(true)
                .macDinh(firstAddress || Boolean.TRUE.equals(request.getMacDinh()))
                .build();

        applyRequest(address, request);

        if (Boolean.TRUE.equals(address.getMacDinh())) {
            diaChiRepository.clearDefaultByCustomerId(customerId);
        }

        DiaChi saved = diaChiRepository.save(address);

        syncLegacyDefaultAddress(customerId);

        return toDto(saved);
    }

    @Transactional
    public DiaChiKhachHangDTO update(
            Integer customerId,
            Integer addressId,
            DiaChiKhachHangRequest request
    ) {
        DiaChi address = findAddress(customerId, addressId);

        boolean wasDefault = Boolean.TRUE.equals(address.getMacDinh());

        applyRequest(address, request);

        if (Boolean.TRUE.equals(request.getMacDinh())) {
            diaChiRepository.clearDefaultByCustomerId(customerId);
            address.setMacDinh(true);
        } else if (wasDefault) {
            address.setMacDinh(true);
        }

        DiaChi saved = diaChiRepository.save(address);

        syncLegacyDefaultAddress(customerId);

        return toDto(saved);
    }

    @Transactional
    public DiaChiKhachHangDTO setDefault(
            Integer customerId,
            Integer addressId
    ) {
        DiaChi address = findAddress(customerId, addressId);

        diaChiRepository.clearDefaultByCustomerId(customerId);

        address.setMacDinh(true);

        DiaChi saved = diaChiRepository.save(address);

        syncLegacyDefaultAddress(customerId);

        return toDto(saved);
    }

    @Transactional
    public void delete(Integer customerId, Integer addressId) {
        DiaChi address = findAddress(customerId, addressId);

        boolean wasDefault = Boolean.TRUE.equals(address.getMacDinh());

        diaChiRepository.delete(address);
        diaChiRepository.flush();

        if (wasDefault) {
            diaChiRepository
                    .findByKhachHang_IdOrderByMacDinhDescIdAsc(customerId)
                    .stream()
                    .findFirst()
                    .ifPresent(nextDefault -> {
                        diaChiRepository.clearDefaultByCustomerId(customerId);

                        nextDefault.setMacDinh(true);

                        diaChiRepository.save(nextDefault);
                    });
        }

        syncLegacyDefaultAddress(customerId);
    }

    private void applyRequest(
            DiaChi address,
            DiaChiKhachHangRequest request
    ) {
        address.setTenDiaChi(clean(request.getTenDiaChi()));
        address.setTenNguoiNhan(clean(request.getTenNguoiNhan()));
        address.setSoDienThoai(normalizePhone(request.getSoDienThoai()));
        address.setThanhPho(clean(request.getThanhPho()));
        // Cột quận/huyện được giữ để đọc dữ liệu cũ, địa chỉ mới dùng mô hình hành chính 2 cấp.
        address.setQuan(null);
        address.setPhuong(clean(request.getPhuong()));
        address.setDiaChiCuThe(clean(request.getDiaChiCuThe()));
    }

    private void syncLegacyDefaultAddress(Integer customerId) {
        KhachHang customer = ensureCustomer(customerId);

        DiaChi defaultAddress = diaChiRepository
                .findFirstByKhachHang_IdAndMacDinhTrueOrderByIdAsc(customerId)
                .orElse(null);

        if (defaultAddress == null) {
            customer.setDiaChi(null);
            customer.setDiaChiChiTiet(null);
            customer.setPhuongXa(null);
            customer.setQuanHuyen(null);
            customer.setTinhThanh(null);
            customer.setPhuongXaCode(null);
            customer.setQuanHuyenCode(null);
            customer.setTinhThanhCode(null);
        } else {
            customer.setDiaChi(fullAddress(defaultAddress));
            customer.setDiaChiChiTiet(defaultAddress.getDiaChiCuThe());
            customer.setPhuongXa(defaultAddress.getPhuong());
            customer.setQuanHuyen(null);
            customer.setTinhThanh(defaultAddress.getThanhPho());
            customer.setPhuongXaCode(null);
            customer.setQuanHuyenCode(null);
            customer.setTinhThanhCode(null);
        }

        khachHangRepository.save(customer);
    }

    private DiaChi findAddress(Integer customerId, Integer addressId) {
        ensureCustomer(customerId);

        return diaChiRepository
                .findByIdAndKhachHang_Id(addressId, customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy địa chỉ của khách hàng"
                        )
                );
    }

    private KhachHang ensureCustomer(Integer customerId) {
        return khachHangRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy khách hàng"
                        )
                );
    }

    private DiaChiKhachHangDTO toDto(DiaChi address) {
        return DiaChiKhachHangDTO.builder()
                .id(address.getId())
                .maDiaChi(address.getMaDiaChi())
                .tenDiaChi(address.getTenDiaChi())
                .tenNguoiNhan(address.getTenNguoiNhan())
                .soDienThoai(address.getSoDienThoai())
                .thanhPho(address.getThanhPho())
                .quan(address.getQuan())
                .phuong(address.getPhuong())
                .diaChiCuThe(address.getDiaChiCuThe())
                .macDinh(Boolean.TRUE.equals(address.getMacDinh()))
                .trangThai(Boolean.TRUE.equals(address.getTrangThai()))
                .diaChiDayDu(fullAddress(address))
                .build();
    }

    private String fullAddress(DiaChi address) {
        return Stream.of(
                        address.getDiaChiCuThe(),
                        address.getPhuong(),
                        address.getThanhPho()
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String generateCode() {
        return "DC" + LocalDateTime.now().format(CODE_FORMAT);
    }

    private String normalizePhone(String value) {
        String phone = value == null ? "" : value.replaceAll("\\D", "");

        if (!phone.matches("^(03|05|07|08|09)\\d{8}$")) {
            throw new IllegalArgumentException(
                    "Số điện thoại người nhận không hợp lệ"
            );
        }

        return phone;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
