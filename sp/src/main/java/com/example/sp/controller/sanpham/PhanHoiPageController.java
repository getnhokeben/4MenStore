package com.example.sp.controller.sanpham;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class PhanHoiPageController {

    @GetMapping(value = {"/phan-hoi", "/phan-hoi.html"}, produces = MediaType.TEXT_HTML_VALUE)
    // Thực hiện xử lý nghiệp vụ của hàm page.
    public ResponseEntity<byte[]> page() throws IOException {
        byte[] html = new ClassPathResource("templates/phan-hoi.html").getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }
}
