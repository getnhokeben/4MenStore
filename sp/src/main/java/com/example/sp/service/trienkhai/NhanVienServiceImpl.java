package com.example.sp.service.trienkhai;

import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.service.tienich.GeneratedCodeUtil;
import com.example.sp.service.nhanvien.EmployeeAccountMailService;
import com.example.sp.service.nhanvien.NhanVienService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NhanVienServiceImpl implements NhanVienService {

    private static final String PHONE_PATTERN = "^(03|05|07|08|09)\\d{8}$";
    private static final String CCCD_PATTERN = "^\\d{12}$";
    private static final String NAME_PATTERN = "^[\\p{L} .'-]+$";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private static final Set<String> VALID_ROLES =
            Set.of("Quản lý", "Nhân viên");

    private static final char[] UPPER =
            "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private static final char[] LOWER =
            "abcdefghijkmnopqrstuvwxyz".toCharArray();

    private static final char[] DIGITS =
            "23456789".toCharArray();

    private static final char[] ALL_PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
                    .toCharArray();

    private final NhanVienRepository nhanVienRepository;
    private final EmployeeAccountMailService employeeAccountMailService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public Page<NhanVien> getAll(
            String keyword,
            String vaiTro,
            Boolean trangThai,
            Pageable pageable
    ) {
        return nhanVienRepository.search(
                trimToNull(keyword),
                trimToNull(vaiTro),
                trangThai,
                withDefaultSort(pageable)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public NhanVien findById(Integer id) {
        return nhanVienRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy nhân viên")
                );
    }

    @Override
    @Transactional
    public NhanVien create(NhanVien nhanVien) {
        normalize(nhanVien);
        validate(nhanVien, null);

        String temporaryPassword = generateTemporaryPassword();

        nhanVien.setId(null);
        nhanVien.setMaNv(generateMaNv(nhanVien.getHoTen()));
        nhanVien.setMatKhau(passwordEncoder.encode(temporaryPassword));
        nhanVien.setTrangThai(true);

        syncLegacyAddress(nhanVien);

        NhanVien saved = nhanVienRepository.saveAndFlush(nhanVien);

        employeeAccountMailService.sendInitialAccount(saved, temporaryPassword);

        return saved;
    }

    @Override
    @Transactional
    public NhanVien update(Integer id, NhanVien request) {
        NhanVien current = findById(id);

        normalize(request);
        validate(request, id);

        current.setHoTen(request.getHoTen());
        current.setSoDienThoai(request.getSoDienThoai());
        current.setEmail(request.getEmail());
        current.setCccd(request.getCccd());
        current.setGioiTinh(request.getGioiTinh());
        current.setNgaySinh(request.getNgaySinh());
        current.setDiaChiChiTiet(request.getDiaChiChiTiet());
        current.setPhuongXa(request.getPhuongXa());
        current.setQuanHuyen(null);
        current.setTinhThanh(request.getTinhThanh());
        current.setVaiTro(request.getVaiTro());
        current.setNgayVaoLam(request.getNgayVaoLam());

        syncLegacyAddress(current);

        return nhanVienRepository.save(current);
    }

    @Override
    @Transactional
    public NhanVien toggleStatus(Integer id) {
        NhanVien employee = findById(id);

        employee.setTrangThai(!Boolean.TRUE.equals(employee.getTrangThai()));

        return nhanVienRepository.save(employee);
    }

    @Override
    @Transactional
    public void deactivate(Integer id) {
        NhanVien employee = findById(id);

        employee.setTrangThai(false);

        nhanVienRepository.save(employee);
    }

    private void validate(NhanVien employee, Integer currentId) {
        LocalDate today = LocalDate.now();

        if (isBlank(employee.getHoTen())) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập họ tên nhân viên"
            );
        }

        if (
                employee.getHoTen().length() < 2 ||
                        employee.getHoTen().length() > 255
        ) {
            throw new IllegalArgumentException(
                    "Họ tên phải từ 2 đến 255 ký tự"
            );
        }

        if (!employee.getHoTen().matches(NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "Họ tên chỉ được chứa chữ cái, khoảng trắng, dấu chấm, nháy đơn hoặc gạch ngang"
            );
        }

        if (isBlank(employee.getEmail())) {
            throw new IllegalArgumentException("Vui lòng nhập email");
        }

        if (!employee.getEmail().matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }

        if (existsEmail(employee.getEmail(), currentId)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        if (isBlank(employee.getSoDienThoai())) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại");
        }

        if (!employee.getSoDienThoai().matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException(
                    "Số điện thoại phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09"
            );
        }

        if (existsPhone(employee.getSoDienThoai(), currentId)) {
            throw new IllegalArgumentException(
                    "Số điện thoại đã được sử dụng"
            );
        }

        if (isBlank(employee.getCccd())) {
            throw new IllegalArgumentException("Vui lòng nhập CCCD");
        }

        if (!employee.getCccd().matches(CCCD_PATTERN)) {
            throw new IllegalArgumentException(
                    "CCCD phải gồm đúng 12 chữ số"
            );
        }

        if (existsCccd(employee.getCccd(), currentId)) {
            throw new IllegalArgumentException("CCCD đã được sử dụng");
        }

        if (!Set.of("Nam", "Nữ", "Khác").contains(employee.getGioiTinh())) {
            throw new IllegalArgumentException("Giới tính không hợp lệ");
        }

        if (employee.getNgaySinh() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày sinh");
        }

        if (!employee.getNgaySinh().isBefore(today)) {
            throw new IllegalArgumentException(
                    "Ngày sinh phải nhỏ hơn ngày hiện tại"
            );
        }

        if (employee.getNgaySinh().plusYears(18).isAfter(today)) {
            throw new IllegalArgumentException(
                    "Nhân viên phải đủ 18 tuổi"
            );
        }

        if (employee.getNgaySinh().isBefore(today.minusYears(130))) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ");
        }

        if (employee.getNgayVaoLam() == null) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn ngày vào làm"
            );
        }

        if (employee.getNgayVaoLam().isAfter(today)) {
            throw new IllegalArgumentException(
                    "Ngày vào làm không được lớn hơn ngày hiện tại"
            );
        }

        if (
                employee.getNgayVaoLam()
                        .isBefore(employee.getNgaySinh().plusYears(18))
        ) {
            throw new IllegalArgumentException(
                    "Ngày vào làm phải từ khi nhân viên đủ 18 tuổi"
            );
        }

        validateAddressPart(
                employee.getTinhThanh(),
                "Vui lòng chọn Tỉnh / Thành"
        );

        validateAddressPart(
                employee.getPhuongXa(),
                "Vui lòng chọn Phường / Xã"
        );

        if (isBlank(employee.getDiaChiChiTiet())) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập số nhà / tên đường"
            );
        }

        if (
                employee.getDiaChiChiTiet().length() < 2 ||
                        employee.getDiaChiChiTiet().length() > 255
        ) {
            throw new IllegalArgumentException(
                    "Số nhà / tên đường phải từ 2 đến 255 ký tự"
            );
        }

        if (
                isBlank(employee.getVaiTro()) ||
                        !VALID_ROLES.contains(employee.getVaiTro())
        ) {
            throw new IllegalArgumentException(
                    "Vai trò chỉ có thể là Quản lý hoặc Nhân viên"
            );
        }
    }

    private void validateAddressPart(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }

        if (value.length() > 255) {
            throw new IllegalArgumentException(
                    "Thông tin địa chỉ không được vượt quá 255 ký tự"
            );
        }
    }

    private boolean existsEmail(String value, Integer currentId) {
        return currentId == null
                ? nhanVienRepository.existsByEmailIgnoreCase(value)
                : nhanVienRepository.existsByEmailIgnoreCaseAndIdNot(
                value,
                currentId
        );
    }

    private boolean existsPhone(String value, Integer currentId) {
        return currentId == null
                ? nhanVienRepository.existsBySoDienThoai(value)
                : nhanVienRepository.existsBySoDienThoaiAndIdNot(
                value,
                currentId
        );
    }

    private boolean existsCccd(String value, Integer currentId) {
        return currentId == null
                ? nhanVienRepository.existsByCccd(value)
                : nhanVienRepository.existsByCccdAndIdNot(
                value,
                currentId
        );
    }

    private String generateMaNv(String name) {
        return GeneratedCodeUtil.fromNameAndDate(
                name,
                null,
                "NV",
                nhanVienRepository::existsByMaNv
        );
    }

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

    private char randomCharacter(char[] source) {
        return source[secureRandom.nextInt(source.length)];
    }

    private Pageable withDefaultSort(Pageable pageable) {
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

    private void normalize(NhanVien employee) {
        employee.setHoTen(normalizeSpaces(employee.getHoTen()));
        employee.setEmail(lower(trimToNull(employee.getEmail())));
        employee.setSoDienThoai(onlyDigits(employee.getSoDienThoai()));
        employee.setCccd(onlyDigits(employee.getCccd()));
        employee.setGioiTinh(trimToNull(employee.getGioiTinh()));
        employee.setDiaChiChiTiet(
                normalizeSpaces(employee.getDiaChiChiTiet())
        );
        employee.setPhuongXa(normalizeSpaces(employee.getPhuongXa()));
        employee.setQuanHuyen(null);
        employee.setTinhThanh(normalizeSpaces(employee.getTinhThanh()));
        employee.setVaiTro(trimToNull(employee.getVaiTro()));

        employee.setMatKhau(null);
    }

    private void syncLegacyAddress(NhanVien employee) {
        employee.setDiaChi(java.util.stream.Stream.of(
                        employee.getDiaChiChiTiet(),
                        employee.getPhuongXa(),
                        employee.getTinhThanh()
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(", ")));
    }

    private String normalizeSpaces(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");

        return normalized.isEmpty() ? null : normalized;
    }

    private String onlyDigits(String value) {
        if (value == null) {
            return null;
        }

        String digits = value.replaceAll("\\D", "");

        return digits.isEmpty() ? null : digits;
    }

    private String lower(String value) {
        return value == null ? null : value.toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
