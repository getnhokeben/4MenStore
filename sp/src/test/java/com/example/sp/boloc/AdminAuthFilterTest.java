package com.example.sp.boloc;

import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthFilterTest {

    private final AdminAuthFilter filter = new AdminAuthFilter();

    @Test
    void rejectsUnauthenticatedManagementApi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/pos/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsStaffFromManagerOnlyApi() throws Exception {
        MockHttpServletRequest request = employeeRequest("/api/nhan-vien", "Nhân viên");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void letsStaffUsePosApi() throws Exception {
        MockHttpServletRequest request = employeeRequest("/api/pos/orders", "Nhân viên");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void leavesPublicShopApiAccessible() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/shop/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    private MockHttpServletRequest employeeRequest(String path, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.getSession().setAttribute(KhoaSessionNhanVien.NHANVIEN_ID, 1);
        request.getSession().setAttribute(KhoaSessionNhanVien.NHANVIEN_VAITRO, role);
        return request;
    }
}
