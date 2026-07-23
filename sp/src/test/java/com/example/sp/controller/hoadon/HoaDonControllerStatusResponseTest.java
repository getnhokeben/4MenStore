package com.example.sp.controller.hoadon;

import com.example.sp.controller.chung.GlobalExceptionHandler;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.service.hoadon.HoaDonService;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HoaDonControllerStatusResponseTest {

    @Test
    void statusUpdateReturnsSafeDtoWithoutSerializingLazyRelations() throws Exception {
        HoaDonService service = mock(HoaDonService.class);
        HoaDonController controller = new HoaDonController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        HoaDon order = HoaDon.builder()
                .id(84)
                .maHoaDon("ANH116072026")
                .trangThai("Đang chuẩn bị hàng")
                .ngayCapNhat(LocalDateTime.of(2026, 7, 16, 20, 33, 40))
                .nhanVien(mock(NhanVien.class))
                .khachHang(mock(KhachHang.class))
                .build();
        when(service.capNhatTrangThai(eq(84), eq("Đang chuẩn bị hàng"), eq(6)))
                .thenReturn(order);

        mockMvc.perform(MockMvcRequestBuilders.put("/hoa-don/84/trang-thai")
                        .sessionAttr(KhoaSessionNhanVien.NHANVIEN_ID, 6)
                        .contentType("application/json")
                        .content("{\"trangThai\":\"Đang chuẩn bị hàng\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(84))
                .andExpect(jsonPath("$.maHoaDon").value("ANH116072026"))
                .andExpect(jsonPath("$.trangThai").value("Đang chuẩn bị hàng"))
                .andExpect(jsonPath("$", not(hasKey("nhanVien"))))
                .andExpect(jsonPath("$", not(hasKey("khachHang"))))
                .andExpect(jsonPath("$", not(hasKey("phieuGiamGia"))));

        verify(service).capNhatTrangThai(84, "Đang chuẩn bị hàng", 6);
    }
}
