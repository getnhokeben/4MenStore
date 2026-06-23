package com.example.sp.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class QuanLyNguoiDungPageController {

    @GetMapping(value = {"/khach-hang", "/khach-hang.html", "/khachHang/hien-thi"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> khachHang() throws IOException {
        return html("templates/khach-hang.html");
    }

    @GetMapping(value = {"/nhan-vien", "/nhan-vien.html", "/nhanVien/hien-thi"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> nhanVien() throws IOException {
        return html("templates/nhan-vien.html");
    }

    private ResponseEntity<byte[]> html(String path) throws IOException {
        byte[] html = new ClassPathResource(path).getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }
}
