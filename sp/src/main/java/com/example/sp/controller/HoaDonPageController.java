package com.example.sp.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class HoaDonPageController {

    @GetMapping(value = {"/hoa-don", "/hoa-don.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> hoaDon() throws IOException {
        return html("templates/hoa-don.html");
    }

    @GetMapping(value = "/hoa-don-chi-tiet.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> hoaDonChiTiet() throws IOException {
        return html("templates/hoa-don-chi-tiet.html");
    }

    private ResponseEntity<byte[]> html(String path) throws IOException {
        byte[] html = new ClassPathResource(path).getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }
}
