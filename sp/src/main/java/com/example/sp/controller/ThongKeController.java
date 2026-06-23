package com.example.sp.controller;

import com.example.sp.dto.ThongKeRequest;
import com.example.sp.dto.DashboardResponse;
import com.example.sp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ThongKeController {

    private final DashboardService dashboardService;

    @GetMapping(value = {"/thong-ke", "/thong-ke.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> view() throws IOException {
        byte[] html = new ClassPathResource("templates/thong-ke.html").getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    @GetMapping("/api/thong-ke")
    public DashboardResponse data(ThongKeRequest req) {
        String from = (req.getFromDate() == null || req.getFromDate().isEmpty()) ? null : req.getFromDate();
        String to   = (req.getToDate()   == null || req.getToDate().isEmpty())   ? null : req.getToDate();
        return dashboardService.getData(from, to);
    }

    @GetMapping("/thong-ke/top-product-ajax")
    @ResponseBody
    public List<Object[]> getTopProductAjax(@RequestParam String type) {
        return dashboardService.getTopProductByType(type);
    }
}
