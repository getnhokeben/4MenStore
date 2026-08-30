package com.example.sp.controller.khachhang;

import com.example.sp.service.cuahang.ShopSessionKeys;
import com.example.sp.service.khachhang.DiaChiKhachHangService;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DiaChiKhachHangControllerAuthorizationTest {

    private final DiaChiKhachHangService service = mock(DiaChiKhachHangService.class);
    private final DiaChiKhachHangController controller = new DiaChiKhachHangController(service);

    @Test
    void rejectsCustomerTryingToReadAnotherCustomersAddress() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ShopSessionKeys.CUSTOMER_ID, 10);

        assertThatThrownBy(() -> controller.findByCustomer(11, session))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(service);
    }

    @Test
    void allowsCustomerToReadOwnAddress() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ShopSessionKeys.CUSTOMER_ID, 10);

        controller.findByCustomer(10, session);

        verify(service).findByCustomer(10);
    }

    @Test
    void allowsEmployeeToAccessCustomerAddressForOrderSupport() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(KhoaSessionNhanVien.NHANVIEN_ID, 7);

        controller.findByCustomer(10, session);

        verify(service).findByCustomer(10);
    }
}
