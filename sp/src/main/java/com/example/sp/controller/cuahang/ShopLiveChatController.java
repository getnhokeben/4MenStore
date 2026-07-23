package com.example.sp.controller.cuahang;

import com.example.sp.dto.cuahang.ShopLiveChatDTO;
import com.example.sp.dto.cuahang.ShopLiveChatMessageRequest;
import com.example.sp.service.cuahang.ShopLiveChatService;
import com.example.sp.service.nhanvien.KhoaSessionNhanVien;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShopLiveChatController {

    private final ShopLiveChatService liveChatService;

    @GetMapping(value = "/chat-ho-tro", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> staffPage() throws IOException {
        byte[] html = new ClassPathResource("templates/ChatHoTro.html").getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    @GetMapping("/api/shop/chat-sessions/current")
    public ResponseEntity<ShopLiveChatDTO> current(HttpSession session) {
        ShopLiveChatDTO dto = liveChatService.currentCustomerSession(session);
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @PostMapping("/api/shop/chat-sessions/new")
    public ResponseEntity<Void> newCustomerSession(HttpSession session) {
        liveChatService.startNewCustomerSession(session);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/shop/chat-sessions/request-staff")
    public ShopLiveChatDTO requestStaff(HttpSession session) {
        return liveChatService.requestStaff(session);
    }

    @PostMapping("/api/shop/chat-sessions/customer/messages")
    public ShopLiveChatDTO customerMessage(@Valid @RequestBody ShopLiveChatMessageRequest request,
                                           HttpSession session) {
        return liveChatService.customerMessage(session, request.getMessage());
    }

    @GetMapping("/api/staff/chat-sessions")
    public ResponseEntity<List<ShopLiveChatDTO>> staffSessions(HttpSession session) {
        if (!isEmployee(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(liveChatService.staffSessions());
    }

    @GetMapping("/api/staff/chat-sessions/{id}")
    public ResponseEntity<ShopLiveChatDTO> staffSession(@PathVariable Integer id,
                                                       HttpSession session) {
        if (!isEmployee(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(liveChatService.staffSession(id));
    }

    @PostMapping("/api/staff/chat-sessions/{id}/messages")
    public ResponseEntity<ShopLiveChatDTO> staffReply(@PathVariable Integer id,
                                                     @Valid @RequestBody ShopLiveChatMessageRequest request,
                                                     HttpSession session) {
        Integer employeeId = employeeId(session);
        if (employeeId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(liveChatService.staffReply(id, employeeId, request.getMessage()));
    }

    @PostMapping("/api/staff/chat-sessions/{id}/close")
    public ResponseEntity<ShopLiveChatDTO> close(@PathVariable Integer id,
                                                HttpSession session) {
        if (!isEmployee(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(liveChatService.closeSession(id));
    }

    private boolean isEmployee(HttpSession session) {
        return employeeId(session) != null;
    }

    private Integer employeeId(HttpSession session) {
        return session == null ? null : (Integer) session.getAttribute(KhoaSessionNhanVien.NHANVIEN_ID);
    }
}
