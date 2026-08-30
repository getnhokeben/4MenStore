package com.example.sp.service.trienkhai;

import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.service.khachhang.CustomerAccountMailService;
import com.example.sp.validation.CustomerNameValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KhachHangServiceImplValidationTest {

    @Mock
    private KhachHangRepository khachHangRepository;

    @Mock
    private NhanVienRepository nhanVienRepository;

    @Mock
    private CustomerAccountMailService customerAccountMailService;

    @InjectMocks
    private KhachHangServiceImpl service;

    @Test
    void createNormalizesDataGeneratesTemporaryPasswordAndAcceptsNonGmailEmail() {
        KhachHang request = validCustomer();
        request.setTenKhachHang("  Nguyễn   Văn   An  ");
        request.setEmail("  AN@outlook.com ");
        request.setSoDienThoai("0901 234 567");
        request.setCccd("001-234-567-890");

        when(khachHangRepository.saveAndFlush(any(KhachHang.class)))
                .thenAnswer(invocation -> {
                    KhachHang customer = invocation.getArgument(0);
                    customer.setId(25);
                    return customer;
                });

        KhachHang saved = service.create(request);

        ArgumentCaptor<KhachHang> customerCaptor =
                ArgumentCaptor.forClass(KhachHang.class);
        ArgumentCaptor<String> passwordCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(khachHangRepository).saveAndFlush(customerCaptor.capture());
        verify(customerAccountMailService).sendInitialAccount(
                eq(saved),
                passwordCaptor.capture()
        );

        KhachHang customer = customerCaptor.getValue();
        assertEquals("Nguyễn Văn An", customer.getTenKhachHang());
        assertEquals("an@outlook.com", customer.getEmail());
        assertEquals("0901234567", customer.getSoDienThoai());
        assertEquals("001234567890", customer.getCccd());
        assertEquals("Khác", customer.getGioiTinh());
        assertTrue(customer.getMaKh().startsWith("NGUYEN"));
        assertTrue(new BCryptPasswordEncoder().matches(
                passwordCaptor.getValue(),
                customer.getMatKhau()
        ));
    }

    @Test
    void createRejectsAnEmailAlreadyAssignedToAnEmployee() {
        KhachHang request = validCustomer();
        when(nhanVienRepository.existsByEmailIgnoreCase("an@outlook.com"))
                .thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request)
        );

        assertEquals("Email đã được sử dụng", error.getMessage());
        verify(khachHangRepository, never()).saveAndFlush(any());
        verify(customerAccountMailService, never())
                .sendInitialAccount(any(), any());
    }

    @Test
    void createRejectsCustomerNamesWithNumbersOrSpecialCharacters() {
        assertInvalidCustomerName("Nguyễn");
        assertInvalidCustomerName("Nguyễn1");
        assertInvalidCustomerName("Nguyễn-An");
        assertInvalidCustomerName("Nguyễn@An");
    }

    private void assertInvalidCustomerName(String name) {
        KhachHang request = validCustomer();
        request.setTenKhachHang(name);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request)
        );

        assertEquals(CustomerNameValidator.INVALID_MESSAGE, error.getMessage());
        verify(khachHangRepository, never()).saveAndFlush(any());
        verify(customerAccountMailService, never())
                .sendInitialAccount(any(), any());
    }

    @Test
    void updateRejectsBirthdayThatIsNotInThePast() {
        KhachHang current = validCustomer();
        current.setId(25);
        KhachHang request = validCustomer();
        request.setNgaySinh(LocalDate.now());

        when(khachHangRepository.findById(25)).thenReturn(Optional.of(current));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.update(25, request)
        );

        assertEquals("Ngày sinh phải nhỏ hơn ngày hiện tại", error.getMessage());
        verify(khachHangRepository, never()).save(any());
    }

    private KhachHang validCustomer() {
        return KhachHang.builder()
                .tenKhachHang("Nguyễn Văn An")
                .email("an@outlook.com")
                .soDienThoai("0901234567")
                .cccd("001234567890")
                .gioiTinh("Khác")
                .ngaySinh(LocalDate.now().minusYears(25))
                .trangThai(true)
                .build();
    }
}
