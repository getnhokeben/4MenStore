package com.example.sp.controller.sanpham;

import com.example.sp.model.sanpham.MauSac;
import com.example.sp.repository.sanpham.MauSacRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ThuocTinhControllerValidationTest {

    @Test
    void createRejectsNameThatOnlyDiffersByCaseOrWhitespace() throws Exception {
        MauSac existing = new MauSac();
        existing.setIdMauSac(1);
        existing.setMaMauSac("MS_DEN");
        existing.setTenMauSac("Đen  tuyền");

        MauSacRepository repository = mock(MauSacRepository.class);
        when(repository.findAll()).thenReturn(List.of(existing));
        ThuocTinhController controller = controllerWith(repository);

        mockMvc(controller).perform(MockMvcRequestBuilders.post("/api/thuoc-tinh/mau-sac")
                        .contentType("application/json")
                        .content("{\"ma\":\"MS_BLACK\",\"ten\":\"  đEN \\t TUYỀN  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đã tồn tại")));
    }

    @Test
    void createStillRejectsDuplicateCode() throws Exception {
        MauSac existing = new MauSac();
        existing.setIdMauSac(1);
        existing.setMaMauSac("MS_DEN");
        existing.setTenMauSac("Đen");

        MauSacRepository repository = mock(MauSacRepository.class);
        when(repository.findAll()).thenReturn(List.of(existing));
        when(repository.save(any(MauSac.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ThuocTinhController controller = controllerWith(repository);

        mockMvc(controller).perform(MockMvcRequestBuilders.post("/api/thuoc-tinh/mau-sac")
                        .contentType("application/json")
                        .content("{\"ma\":\"ms_den\",\"ten\":\"Trắng\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Mã thuộc tính")));
    }

    private ThuocTinhController controllerWith(MauSacRepository repository) {
        ThuocTinhController controller = new ThuocTinhController();
        ReflectionTestUtils.setField(controller, "mauSacRepo", repository);
        return controller;
    }

    private MockMvc mockMvc(ThuocTinhController controller) {
        return MockMvcBuilders.standaloneSetup(controller).build();
    }
}
