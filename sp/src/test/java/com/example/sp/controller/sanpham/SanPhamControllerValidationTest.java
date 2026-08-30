package com.example.sp.controller.sanpham;

import com.example.sp.controller.chung.GlobalExceptionHandler;
import com.example.sp.dto.sanpham.SanPhamFullRequest;
import com.example.sp.model.sanpham.SanPham;
import com.example.sp.service.sanpham.SanPhamService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SanPhamControllerValidationTest {

    @Test
    void updateProductAllowsOmittedVariantsToPreserveExistingVariants() throws Exception {
        SanPhamService service = mock(SanPhamService.class);
        SanPhamController controller = new SanPhamController();
        ReflectionTestUtils.setField(controller, "sanPhamService", service);

        SanPham updated = new SanPham();
        updated.setIdSp(1);
        when(service.updateProduct(eq(1), any(SanPhamFullRequest.class)))
                .thenReturn(updated);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(MockMvcRequestBuilders.put("/san-pham/sua/1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "maSp": "SP001",
                                  "tenSp": "Ao so mi"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSp").value(1));

        verify(service).updateProduct(eq(1), any(SanPhamFullRequest.class));
    }

    @Test
    void updateProductValidatesNestedVariantsBeforeCallingService() throws Exception {
        SanPhamService service = mock(SanPhamService.class);
        SanPhamController controller = new SanPhamController();
        ReflectionTestUtils.setField(controller, "sanPhamService", service);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String payload = """
                {
                  "maSp": "SP001",
                  "tenSp": "Ao so mi",
                  "danhSachBienThe": [
                    {
                      "maChiTietSanPham": "SP001-M",
                      "idMauSac": 1,
                      "idLoaiAo": 1,
                      "idPhongCachMac": 1,
                      "idKieuDang": 1,
                      "soLuongTon": 5,
                      "giaNhap": 100000,
                      "donGia": 150000
                    }
                  ]
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.put("/san-pham/sua/1")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", hasKey("danhSachBienThe[0].idKichCo")));

        verifyNoInteractions(service);
    }
}
