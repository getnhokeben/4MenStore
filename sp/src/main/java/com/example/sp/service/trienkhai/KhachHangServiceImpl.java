package com.example.sp.service.trienkhai;

import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.service.tienich.GeneratedCodeUtil;
import com.example.sp.service.khachhang.KhachHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KhachHangServiceImpl implements KhachHangService {

    private static final String PHONE_PATTERN = "^(03|05|07|08|09)\\d{8}$";
    private static final String CCCD_PATTERN = "^\\d{12}$";
    private static final String NAME_PATTERN = "^[\\p{L}][\\p{L} .'-]*$";

    private final KhachHangRepository khachHangRepository;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Override
    @Transactional(readOnly = true)
    public Page<KhachHang> getAll(
            String keyword,
            Boolean trangThai,
            String gioiTinh,
            Pageable pageable
    ) {
        return khachHangRepository.search(
                trimToNull(keyword),
                trangThai,
                trimToNull(gioiTinh),
                defaultSort(pageable)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public KhachHang findById(Integer id) {
        return khachHangRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy khách hàng")
                );
    }

    @Override
    @Transactional
    public KhachHang create(KhachHang customer) {
        normalize(customer);
        validate(customer, null, true);

        customer.setId(null);
        customer.setMaKh(generateCustomerCode(customer.getTenKhachHang()));
        customer.setTenTaiKhoan(customer.getEmail());
        customer.setMatKhau(passwordEncoder.encode(customer.getMatKhau()));
        customer.setTrangThai(
                customer.getTrangThai() == null || customer.getTrangThai()
        );

        return khachHangRepository.save(customer);
    }

    @Override
    @Transactional
    public KhachHang update(Integer id, KhachHang request) {
        KhachHang current = findById(id);

        normalize(request);
        validate(request, id, false);

        current.setTenKhachHang(request.getTenKhachHang());
        current.setTenTaiKhoan(request.getEmail());
        current.setSoDienThoai(request.getSoDienThoai());
        current.setEmail(request.getEmail());
        current.setCccd(request.getCccd());
        current.setGioiTinh(request.getGioiTinh());
        current.setNgaySinh(request.getNgaySinh());

        if (request.getTrangThai() != null) {
            current.setTrangThai(request.getTrangThai());
        }

        if (!isBlank(request.getMatKhau())) {
            current.setMatKhau(
                    passwordEncoder.encode(request.getMatKhau())
            );
        }

        return khachHangRepository.save(current);
    }

    @Override
    @Transactional
    public KhachHang toggleStatus(Integer id) {
        KhachHang customer = findById(id);

        customer.setTrangThai(!Boolean.TRUE.equals(customer.getTrangThai()));

        return khachHangRepository.save(customer);
    }

    @Override
    @Transactional
    public void deactivate(Integer id) {
        KhachHang customer = findById(id);

        customer.setTrangThai(false);

        khachHangRepository.save(customer);
    }

    private void validate(
            KhachHang customer,
            Integer currentId,
            boolean creating
    ) {
        if (isBlank(customer.getTenKhachHang())) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập họ tên khách hàng"
            );
        }

        if (!customer.getTenKhachHang().matches(NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "Họ tên chỉ được chứa chữ cái, khoảng trắng, dấu chấm, nháy đơn hoặc gạch ngang"
            );
        }

        if (
                isBlank(customer.getSoDienThoai())
                        || !customer.getSoDienThoai().matches(PHONE_PATTERN)
        ) {
            throw new IllegalArgumentException(
                    "Số điện thoại chính không hợp lệ"
            );
        }

        if (existsPhone(customer.getSoDienThoai(), currentId)) {
            throw new IllegalArgumentException(
                    "Số điện thoại chính đã được sử dụng"
            );
        }

        if (isBlank(customer.getEmail())) {
            throw new IllegalArgumentException("Vui long nhap email");
        }

        if (!customer.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email khong hop le");
        }

        if (existsEmail(customer.getEmail(), currentId)) {
            throw new IllegalArgumentException("Email da duoc su dung");
        }

        if (!isBlank(customer.getCccd())) {
            if (!customer.getCccd().matches(CCCD_PATTERN)) {
                throw new IllegalArgumentException(
                        "CCCD phải gồm đúng 12 chữ số"
                );
            }

            if (existsCccd(customer.getCccd(), currentId)) {
                throw new IllegalArgumentException(
                        "CCCD đã được sử dụng"
                );
            }
        }
    }

    private boolean existsPhone(String value, Integer currentId) {
        return currentId == null
                ? khachHangRepository.existsBySoDienThoai(value)
                : khachHangRepository.existsBySoDienThoaiAndIdNot(
                value,
                currentId
        );
    }

    private boolean existsEmail(String value, Integer currentId) {
        return currentId == null
                ? khachHangRepository.existsByEmailIgnoreCase(value)
                : khachHangRepository.existsByEmailIgnoreCaseAndIdNot(
                value,
                currentId
        );
    }

    private boolean existsCccd(String value, Integer currentId) {
        return currentId == null
                ? khachHangRepository.existsByCccd(value)
                : khachHangRepository.existsByCccdAndIdNot(value, currentId);
    }

    private Pageable defaultSort(Pageable pageable) {
        Sort sort = Sort.by(
                Sort.Order.desc("trangThai"),
                Sort.Order.desc("id")
        );

        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged(sort);
        }

        if (pageable.getSort().isSorted()) {
            return pageable;
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }

    private String generateCustomerCode(String name) {
        return GeneratedCodeUtil.fromNameAndDate(
                name,
                null,
                "KH",
                khachHangRepository::existsByMaKh
        );
    }

    private void normalize(KhachHang customer) {
        customer.setTenKhachHang(trimToNull(customer.getTenKhachHang()));
        customer.setSoDienThoai(normalizePhone(customer.getSoDienThoai()));
        customer.setEmail(lower(trimToNull(customer.getEmail())));
        customer.setTenTaiKhoan(customer.getEmail());
        customer.setCccd(trimToNull(customer.getCccd()));
        customer.setGioiTinh(trimToNull(customer.getGioiTinh()));
        customer.setMatKhau(trimToNull(customer.getMatKhau()));
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        String normalized = phone.replaceAll("\\D", "");

        return normalized.isBlank() ? null : normalized;
    }

    private String lower(String value) {
        return value == null ? null : value.toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
