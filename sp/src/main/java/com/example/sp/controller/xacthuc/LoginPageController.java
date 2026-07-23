package com.example.sp.controller.xacthuc;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class LoginPageController {

    @GetMapping(value = {"/dang-nhap.html", "/dang-nhap"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> dangNhap() throws IOException {
        return html("templates/dang-nhap.html");
    }

    private ResponseEntity<byte[]> html(String path) throws IOException {
        byte[] html = new ClassPathResource(path).getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }
}

