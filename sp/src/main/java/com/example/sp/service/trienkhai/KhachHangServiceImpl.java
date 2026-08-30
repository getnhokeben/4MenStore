package com.example.sp.service.trienkhai;

import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.service.tienich.GeneratedCodeUtil;
import com.example.sp.validation.CustomerNameValidator;
import com.example.sp.service.khachhang.CustomerAccountMailService;
import com.example.sp.service.khachhang.KhachHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KhachHangServiceImpl implements KhachHangService {

    private static final String PHONE_PATTERN = "^(03|05|07|08|09)\\d{8}$";
    private static final String CCCD_PATTERN = "^\\d{12}$";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final Set<String> VALID_GENDERS =
            Set.of("Nam", "Nữ", "Khác");
    private static final char[] UPPER =
            "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] LOWER =
            "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] ALL_PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
                    .toCharArray();

    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final CustomerAccountMailService customerAccountMailService;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    // Tải hoặc truy xuất dữ liệu cho get all.
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
    // Tải hoặc truy xuất dữ liệu cho find by id.
    public KhachHang findById(Integer id) {
        return khachHangRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy khách hàng")
                );
    }

    @Override
    @Transactional
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho create.
    public KhachHang create(KhachHang customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Dữ liệu khách hàng không hợp lệ");
        }

        normalize(customer);
        validate(customer, null);

        String temporaryPassword = generateTemporaryPassword();

        customer.setId(null);
        customer.setMaKh(generateCustomerCode(customer.getTenKhachHang()));
        customer.setTenTaiKhoan(customer.getEmail());
        customer.setMatKhau(passwordEncoder.encode(temporaryPassword));
        customer.setTrangThai(true);

        KhachHang saved = khachHangRepository.saveAndFlush(customer);

        customerAccountMailService.sendInitialAccount(
                saved,
                temporaryPassword
        );

        return saved;
    }

    @Override
    @Transactional
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho update.
    public KhachHang update(Integer id, KhachHang request) {
        KhachHang current = findById(id);

        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu khách hàng không hợp lệ");
        }

        normalize(request);
        validate(request, id);

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

        return khachHangRepository.save(current);
    }

    @Override
    @Transactional
    // Xử lý tương tác người dùng cho toggle status.
    public KhachHang toggleStatus(Integer id) {
        KhachHang customer = findById(id);

        customer.setTrangThai(!Boolean.TRUE.equals(customer.getTrangThai()));

        return khachHangRepository.save(customer);
    }

    @Override
    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm deactivate.
    public void deactivate(Integer id) {
        KhachHang customer = findById(id);

        customer.setTrangThai(false);

        khachHangRepository.save(customer);
    }

    // Kiểm tra điều kiện và tính hợp lệ cho validate.
    private void validate(KhachHang customer, Integer currentId) {
        LocalDate today = LocalDate.now();

        if (isBlank(customer.getTenKhachHang())) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập họ tên khách hàng"
            );
        }

        if (customer.getTenKhachHang().length() < 2
                || customer.getTenKhachHang().length() > 255) {
            throw new IllegalArgumentException(
                    "Họ tên phải từ 2 đến 255 ký tự"
            );
        }

        if (!CustomerNameValidator.isValid(customer.getTenKhachHang())) {
            throw new IllegalArgumentException(
                    CustomerNameValidator.INVALID_MESSAGE
            );
        }

        if (isBlank(customer.getEmail())) {
            throw new IllegalArgumentException("Vui lòng nhập email");
        }

        if (customer.getEmail().length() > 255
                || !customer.getEmail().matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }

        if (existsEmail(customer.getEmail(), currentId)
                || nhanVienRepository.existsByEmailIgnoreCase(
                customer.getEmail()
        )) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        if (isBlank(customer.getSoDienThoai())) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập số điện thoại chính"
            );
        }

        if (!customer.getSoDienThoai().matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException(
                    "Số điện thoại chính phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09"
            );
        }

        if (existsPhone(customer.getSoDienThoai(), currentId)) {
            throw new IllegalArgumentException(
                    "Số điện thoại chính đã được sử dụng"
            );
        }

        if (isBlank(customer.getCccd())) {
            throw new IllegalArgumentException("Vui lòng nhập CCCD");
        }

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

        if (!VALID_GENDERS.contains(customer.getGioiTinh())) {
            throw new IllegalArgumentException(
                    "Giới tính chỉ có thể là Nam, Nữ hoặc Khác"
            );
        }

        if (customer.getNgaySinh() != null) {
            if (!customer.getNgaySinh().isBefore(today)) {
                throw new IllegalArgumentException(
                        "Ngày sinh phải nhỏ hơn ngày hiện tại"
                );
            }

            if (customer.getNgaySinh().isBefore(today.minusYears(130))) {
                throw new IllegalArgumentException("Ngày sinh không hợp lệ");
            }
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm exists phone.
    private boolean existsPhone(String value, Integer currentId) {
        return currentId == null
                ? khachHangRepository.existsBySoDienThoai(value)
                : khachHangRepository.existsBySoDienThoaiAndIdNot(
                value,
                currentId
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm exists email.
    private boolean existsEmail(String value, Integer currentId) {
        return currentId == null
                ? khachHangRepository.existsByEmailIgnoreCase(value)
                : khachHangRepository.existsByEmailIgnoreCaseAndIdNot(
                value,
                currentId
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm exists cccd.
    private boolean existsCccd(String value, Integer currentId) {
        return currentId == null
                ? khachHangRepository.existsByCccd(value)
                : khachHangRepository.existsByCccdAndIdNot(value, currentId);
    }

    // Thực hiện xử lý nghiệp vụ của hàm default sort.
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

    // Thực hiện xử lý nghiệp vụ của hàm generate customer code.
    private String generateCustomerCode(String name) {
        return GeneratedCodeUtil.fromNameAndDate(
                name,
                null,
                "KH",
                khachHangRepository::existsByMaKh
        );
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize.
    private void normalize(KhachHang customer) {
        customer.setTenKhachHang(
                CustomerNameValidator.normalize(customer.getTenKhachHang())
        );
        customer.setSoDienThoai(onlyDigits(customer.getSoDienThoai()));
        customer.setEmail(lower(trimToNull(customer.getEmail())));
        customer.setTenTaiKhoan(customer.getEmail());
        customer.setCccd(onlyDigits(customer.getCccd()));
        customer.setGioiTinh(trimToNull(customer.getGioiTinh()));
        customer.setMatKhau(null);
    }

    // Thực hiện xử lý nghiệp vụ của hàm generate temporary password.
    private String generateTemporaryPassword() {
        List<Character> characters = new ArrayList<>();

        characters.add(randomCharacter(UPPER));
        characters.add(randomCharacter(LOWER));
        characters.add(randomCharacter(DIGITS));

        for (int index = characters.size(); index < 6; index++) {
            characters.add(randomCharacter(ALL_PASSWORD_CHARACTERS));
        }

        Collections.shuffle(characters, secureRandom);

        StringBuilder password = new StringBuilder();
        for (char character : characters) {
            password.append(character);
        }

        return password.toString();
    }

    // Thực hiện xử lý nghiệp vụ của hàm random character.
    private char randomCharacter(char[] source) {
        return source[secureRandom.nextInt(source.length)];
    }

    // Thực hiện xử lý nghiệp vụ của hàm normalize spaces.
    private String normalizeSpaces(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");

        return normalized.isEmpty() ? null : normalized;
    }

    // Thực hiện xử lý nghiệp vụ của hàm only digits.
    private String onlyDigits(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.replaceAll("\\D", "");

        return normalized.isBlank() ? null : normalized;
    }

    // Thực hiện xử lý nghiệp vụ của hàm lower.
    private String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is blank.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // Thực hiện xử lý nghiệp vụ của hàm trim to null.
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
