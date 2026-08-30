package com.example.sp.service.sanpham;

import com.example.sp.dto.sanpham.SanPhamFullRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SanPhamServiceValidationTest {

    @Test
    void createProductRequiresAtLeastOneVariant() {
        SanPhamFullRequest request = new SanPhamFullRequest();
        request.setMaSp("SP001");
        request.setTenSp("Áo sơ mi");

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> new SanPhamService().createProduct(request)
        );

        assertEquals(
                "Sản phẩm phải có ít nhất một biến thể chi tiết",
                error.getMessage()
        );
    }
}
