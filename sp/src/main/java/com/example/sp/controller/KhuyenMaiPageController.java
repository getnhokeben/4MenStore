package com.example.sp.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class KhuyenMaiPageController {

    @GetMapping(value = {"/phieu-giam-gia", "/phieu-giam-gia.html", "/voucher"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> phieuGiamGia() throws IOException {
        return html("templates/phieu-giam-gia.html");
    }

    @GetMapping(value = {"/dot-giam-gia", "/dot-giam-gia.html", "/promotion"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> dotGiamGia() throws IOException {
        return html("templates/dot-giam-gia.html");
    }

    private ResponseEntity<byte[]> html(String path) throws IOException {
        byte[] html = new ClassPathResource(path).getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }
}
