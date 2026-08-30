package com.example.sp.service.cuahang;

import com.example.sp.dto.cuahang.ShopCustomerDTO;
import com.example.sp.dto.cuahang.ShopRegisterRequest;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.validation.CustomerNameValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTest {

    @Mock
    private KhachHangRepository khachHangRepository;

    @Mock
    private NhanVienRepository nhanVienRepository;

    @InjectMocks
    private CustomerAuthService service;

    @Test
    void registerCreatesAnActiveCustomerAndHashesThePassword() {
        ShopRegisterRequest request = request(
                "  Nguyễn Văn An  ",
                "0901 234 567",
                "  AN@example.com ",
                "  12 Nguyễn Huệ, Quận 1  ",
                "mat-khau-moi"
        );

        when(khachHangRepository.save(any(KhachHang.class)))
                .thenAnswer(invocation -> {
                    KhachHang customer = invocation.getArgument(0);
                    customer.setId(21);
                    return customer;
                });

        ShopCustomerDTO result = service.register(request);

        ArgumentCaptor<KhachHang> customerCaptor =
                ArgumentCaptor.forClass(KhachHang.class);
        verify(khachHangRepository).save(customerCaptor.capture());

        KhachHang customer = customerCaptor.getValue();
        assertEquals("Nguyễn Văn An", customer.getTenKhachHang());
        assertEquals("0901234567", customer.getSoDienThoai());
        assertEquals("an@example.com", customer.getEmail());
        assertEquals("12 Nguyễn Huệ, Quận 1", customer.getDiaChi());
        assertTrue(customer.getTrangThai());
        assertTrue(new BCryptPasswordEncoder().matches(
                "mat-khau-moi",
                customer.getMatKhau()
        ));
        assertEquals(21, result.getId());
        assertEquals("an@example.com", result.getEmail());
        assertEquals("12 Nguyễn Huệ, Quận 1", result.getDiaChi());
    }

    @Test
    void registerRejectsAnEmailAlreadyAssignedToAnEmployee() {
        ShopRegisterRequest request = request(
                "Nguyễn Văn An",
                "0901234567",
                "an@example.com",
                "",
                "mat-khau-moi"
        );
        when(nhanVienRepository.existsByEmailIgnoreCase("an@example.com"))
                .thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(request)
        );

        assertEquals("Email đã được sử dụng", error.getMessage());
        verify(khachHangRepository, never()).save(any(KhachHang.class));
    }

    @Test
    void registerRejectsAnInvalidVietnamesePhoneNumber() {
        ShopRegisterRequest request = request(
                "Nguyễn Văn An",
                "0123456789",
                "an@example.com",
                "",
                "mat-khau-moi"
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(request)
        );

        assertEquals(
                "Số điện thoại không đúng định dạng Việt Nam",
                error.getMessage()
        );
        assertFalse(error.getMessage().isBlank());
        verify(khachHangRepository, never()).save(any(KhachHang.class));
    }

    @Test
    void registerRejectsCustomerNamesWithNumbersOrSpecialCharacters() {
        assertInvalidCustomerName("Nguyễn");
        assertInvalidCustomerName("Nguyễn1");
        assertInvalidCustomerName("Nguyễn-An");
        assertInvalidCustomerName("Nguyễn@An");
    }

    private void assertInvalidCustomerName(String name) {
        ShopRegisterRequest request = request(
                name,
                "0901234567",
                "an@example.com",
                "",
                "mat-khau-moi"
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(request)
        );

        assertEquals(CustomerNameValidator.INVALID_MESSAGE, error.getMessage());
        verify(khachHangRepository, never()).save(any(KhachHang.class));
    }

    private ShopRegisterRequest request(
            String name,
            String phone,
            String email,
            String address,
            String password
    ) {
        ShopRegisterRequest request = new ShopRegisterRequest();
        request.setTenKhachHang(name);
        request.setSoDienThoai(phone);
        request.setEmail(email);
        request.setDiaChi(address);
        request.setMatKhau(password);
        return request;
    }
}
