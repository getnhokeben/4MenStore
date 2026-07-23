package com.example.sp.service.cuahang;

import com.example.sp.dto.cuahang.ShopLiveChatDTO;
import com.example.sp.model.cuahang.ShopChatMessage;
import com.example.sp.model.cuahang.ShopChatSession;
import com.example.sp.model.khachhang.KhachHang;
import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.repository.cuahang.ShopChatMessageRepository;
import com.example.sp.repository.cuahang.ShopChatSessionRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopLiveChatService {

    private static final String SESSION_PREFIX = "SHOP_CHAT_";
    private static final String STATUS_AI = "AI";
    private static final String STATUS_WAITING = "WAITING_STAFF";
    private static final String STATUS_STAFF = "STAFF_JOINED";
    private static final String STATUS_CLOSED = "CLOSED";

    private final ShopChatSessionRepository sessionRepo;
    private final ShopChatMessageRepository messageRepo;
    private final KhachHangRepository khachHangRepo;
    private final NhanVienRepository nhanVienRepo;

    @Transactional
    public ShopChatSession getOrCreateCustomerSession(HttpSession httpSession) {
        String key = chatKey(httpSession);
        return sessionRepo.findBySessionKey(key).orElseGet(() -> {
            ShopChatSession session = ShopChatSession.builder()
                    .sessionKey(key)
                    .trangThai(STATUS_AI)
                    .build();

            Integer customerId = (Integer) httpSession.getAttribute(ShopSessionKeys.CUSTOMER_ID);
            if (customerId != null) {
                khachHangRepo.findById(customerId).ifPresent(customer -> applyCustomer(session, customer));
            }

            if (session.getTenKhachHang() == null || session.getTenKhachHang().isBlank()) {
                session.setTenKhachHang("Khách online");
            }

            return sessionRepo.save(session);
        });
    }

    @Transactional
    public void recordAiExchange(HttpSession httpSession, String userMessage, String assistantReply) {
        ShopChatSession session = getOrCreateCustomerSession(httpSession);
        if (STATUS_CLOSED.equals(session.getTrangThai())) {
            startNewCustomerSession(httpSession);
            session = getOrCreateCustomerSession(httpSession);
        }
        addMessage(session, "CUSTOMER", displayCustomerName(session), userMessage, false);
        addMessage(session, "AI", "Trợ lý AI", assistantReply, true);
    }

    @Transactional
    public void startNewCustomerSession(HttpSession httpSession) {
        if (httpSession == null) return;
        httpSession.setAttribute("SHOP_CHAT_SESSION_KEY", SESSION_PREFIX + httpSession.getId() + "_" + UUID.randomUUID());
    }

    @Transactional
    public ShopLiveChatDTO requestStaff(HttpSession httpSession) {
        ShopChatSession session = getOrCreateCustomerSession(httpSession);
        if (STATUS_CLOSED.equals(session.getTrangThai())) {
            session.setTrangThai(STATUS_WAITING);
            session.setNgayDong(null);
        } else if (STATUS_AI.equals(session.getTrangThai())) {
            session.setTrangThai(STATUS_WAITING);
        }
        addMessage(session, "SYSTEM", "4MenStore", "Khách đã yêu cầu gặp nhân viên.", false);
        return toDTO(sessionRepo.save(session), true);
    }

    @Transactional
    public ShopLiveChatDTO customerMessage(HttpSession httpSession, String message) {
        ShopChatSession session = getOrCreateCustomerSession(httpSession);
        if (STATUS_CLOSED.equals(session.getTrangThai())) {
            session.setTrangThai(STATUS_WAITING);
            session.setNgayDong(null);
        }
        if (STATUS_AI.equals(session.getTrangThai())) {
            session.setTrangThai(STATUS_WAITING);
        }
        addMessage(session, "CUSTOMER", displayCustomerName(session), message, false);
        return toDTO(sessionRepo.save(session), true);
    }

    @Transactional(readOnly = true)
    public ShopLiveChatDTO currentCustomerSession(HttpSession httpSession) {
        return sessionRepo.findBySessionKey(chatKey(httpSession))
                .map(session -> toDTO(session, true))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ShopLiveChatDTO> staffSessions() {
        return sessionRepo.findByTrangThaiInOrderByNgayCapNhatDesc(List.of(STATUS_WAITING, STATUS_STAFF))
                .stream()
                .map(session -> toDTO(session, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public ShopLiveChatDTO staffSession(Integer id) {
        return toDTO(requireSession(id), true);
    }

    @Transactional
    public ShopLiveChatDTO staffReply(Integer id, Integer employeeId, String message) {
        ShopChatSession session = requireSession(id);
        NhanVien employee = nhanVienRepo.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));
        session.setTrangThai(STATUS_STAFF);
        session.setIdNhanVien(employee.getId());
        session.setTenNhanVien(employee.getHoTen());
        addMessage(session, "STAFF", employee.getHoTen(), message, false);
        return toDTO(sessionRepo.save(session), true);
    }

    @Transactional
    public ShopLiveChatDTO closeSession(Integer id) {
        ShopChatSession session = requireSession(id);
        session.setTrangThai(STATUS_CLOSED);
        session.setNgayDong(LocalDateTime.now());
        addMessage(session, "SYSTEM", "4MenStore", "Nhân viên đã kết thúc phiên chat.", false);
        return toDTO(sessionRepo.save(session), true);
    }

    private void applyCustomer(ShopChatSession session, KhachHang customer) {
        session.setIdKhachHang(customer.getId());
        session.setTenKhachHang(customer.getTenKhachHang());
        session.setEmail(customer.getEmail());
    }

    private String chatKey(HttpSession session) {
        Object existing = session.getAttribute("SHOP_CHAT_SESSION_KEY");
        if (existing != null && !String.valueOf(existing).isBlank()) return String.valueOf(existing);
        String key = SESSION_PREFIX + session.getId();
        session.setAttribute("SHOP_CHAT_SESSION_KEY", key);
        return key;
    }

    private String displayCustomerName(ShopChatSession session) {
        return session.getTenKhachHang() == null || session.getTenKhachHang().isBlank()
                ? "Khách online"
                : session.getTenKhachHang();
    }

    private void addMessage(ShopChatSession session, String senderType, String senderName, String content, boolean aiGenerated) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isBlank()) return;
        messageRepo.save(ShopChatMessage.builder()
                .idSession(session.getId())
                .senderType(senderType)
                .senderName(senderName)
                .noiDung(trimmed)
                .aiGenerated(aiGenerated)
                .build());
        session.setNgayCapNhat(LocalDateTime.now());
    }

    private ShopChatSession requireSession(Integer id) {
        return sessionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên chat"));
    }

    private ShopLiveChatDTO toDTO(ShopChatSession session, boolean includeMessages) {
        List<ShopLiveChatDTO.Message> messages = includeMessages
                ? messageRepo.findByIdSessionOrderByNgayTaoAscIdAsc(session.getId())
                        .stream()
                        .map(this::toMessageDTO)
                        .toList()
                : List.of();
        return ShopLiveChatDTO.builder()
                .id(session.getId())
                .status(session.getTrangThai())
                .customerName(session.getTenKhachHang())
                .customerEmail(session.getEmail())
                .employeeName(session.getTenNhanVien())
                .createdAt(session.getNgayTao())
                .updatedAt(session.getNgayCapNhat())
                .messages(messages)
                .build();
    }

    private ShopLiveChatDTO.Message toMessageDTO(ShopChatMessage message) {
        return ShopLiveChatDTO.Message.builder()
                .id(message.getId())
                .senderType(message.getSenderType())
                .senderName(message.getSenderName())
                .content(message.getNoiDung())
                .aiGenerated(message.getAiGenerated())
                .createdAt(message.getNgayTao())
                .build();
    }
}
