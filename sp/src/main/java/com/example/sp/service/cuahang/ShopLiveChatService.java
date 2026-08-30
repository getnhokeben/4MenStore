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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final Duration SESSION_DURATION = Duration.ofMinutes(30);

    private final ShopChatSessionRepository sessionRepo;
    private final ShopChatMessageRepository messageRepo;
    private final KhachHangRepository khachHangRepo;
    private final NhanVienRepository nhanVienRepo;

    @Transactional
    // Tải hoặc truy xuất dữ liệu cho get or create customer session.
    public ShopChatSession getOrCreateCustomerSession(HttpSession httpSession) {
        String key = chatKey(httpSession);
        return sessionRepo.findBySessionKey(key).map(session -> {
            expireIfNeeded(session);
            return session;
        }).orElseGet(() -> {
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
    // Thực hiện xử lý nghiệp vụ của hàm record ai exchange.
    public void recordAiExchange(HttpSession httpSession, String userMessage, String assistantReply) {
        ShopChatSession session = activeCustomerSession(httpSession);
        addMessage(session, "CUSTOMER", displayCustomerName(session), userMessage, false);
        addMessage(session, "AI", "Trợ lý AI", assistantReply, true);
    }

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm start new customer session.
    public void startNewCustomerSession(HttpSession httpSession) {
        if (httpSession == null) return;
        httpSession.setAttribute("SHOP_CHAT_SESSION_KEY", SESSION_PREFIX + httpSession.getId() + "_" + UUID.randomUUID());
    }

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm request staff.
    public ShopLiveChatDTO requestStaff(HttpSession httpSession) {
        ShopChatSession session = activeCustomerSession(httpSession);
        if (STATUS_AI.equals(session.getTrangThai())) {
            session.setTrangThai(STATUS_WAITING);
        }
        addMessage(session, "SYSTEM", "4MenStore", "Khách đã yêu cầu gặp nhân viên.", false);
        return toDTO(sessionRepo.save(session), true);
    }

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm customer message.
    public ShopLiveChatDTO customerMessage(HttpSession httpSession, String message) {
        ShopChatSession session = activeCustomerSession(httpSession);
        if (STATUS_AI.equals(session.getTrangThai())) {
            session.setTrangThai(STATUS_WAITING);
        }
        addMessage(session, "CUSTOMER", displayCustomerName(session), message, false);
        return toDTO(sessionRepo.save(session), true);
    }

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm current customer session.
    public ShopLiveChatDTO currentCustomerSession(HttpSession httpSession) {
        return sessionRepo.findBySessionKey(chatKey(httpSession))
                .map(session -> {
                    expireIfNeeded(session);
                    return toDTO(session, true);
                })
                .orElse(null);
    }

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm staff sessions.
    public List<ShopLiveChatDTO> staffSessions() {
        return sessionRepo.findAllByOrderByNgayCapNhatDesc()
                .stream()
                .peek(this::expireIfNeeded)
                .map(session -> toDTO(session, false))
                .toList();
    }

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm staff session.
    public ShopLiveChatDTO staffSession(Integer id) {
        ShopChatSession session = requireSession(id);
        expireIfNeeded(session);
        return toDTO(session, true);
    }

    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm staff reply.
    public ShopLiveChatDTO staffReply(Integer id, Integer employeeId, String message) {
        ShopChatSession session = requireSession(id);
        ensureSessionCanReceiveMessages(session);
        NhanVien employee = nhanVienRepo.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));
        session.setTrangThai(STATUS_STAFF);
        session.setIdNhanVien(employee.getId());
        session.setTenNhanVien(employee.getHoTen());
        addMessage(session, "STAFF", employee.getHoTen(), message, false);
        return toDTO(sessionRepo.save(session), true);
    }

    @Transactional
    // Xử lý thao tác đóng, xóa hoặc hủy cho close session.
    public ShopLiveChatDTO closeSession(Integer id) {
        ShopChatSession session = requireSession(id);
        expireIfNeeded(session);
        if (STATUS_EXPIRED.equals(session.getTrangThai())) return toDTO(session, true);
        session.setTrangThai(STATUS_CLOSED);
        session.setNgayDong(LocalDateTime.now());
        addMessage(session, "SYSTEM", "4MenStore", "Nhân viên đã kết thúc phiên chat.", false);
        return toDTO(sessionRepo.save(session), true);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    // Thực hiện xử lý nghiệp vụ của hàm expire timed out sessions.
    public void expireTimedOutSessions() {
        sessionRepo.findByTrangThaiInOrderByNgayCapNhatDesc(List.of(STATUS_AI, STATUS_WAITING, STATUS_STAFF))
                .forEach(this::expireIfNeeded);
    }

    // Thực hiện xử lý nghiệp vụ của hàm active customer session.
    private ShopChatSession activeCustomerSession(HttpSession httpSession) {
        ShopChatSession session = getOrCreateCustomerSession(httpSession);
        if (isActive(session)) return session;
        startNewCustomerSession(httpSession);
        return getOrCreateCustomerSession(httpSession);
    }

    // Thực hiện xử lý nghiệp vụ của hàm ensure session can receive messages.
    private void ensureSessionCanReceiveMessages(ShopChatSession session) {
        expireIfNeeded(session);
        if (STATUS_EXPIRED.equals(session.getTrangThai())) {
            throw new IllegalStateException("Phiên chat đã hết hạn sau 30 phút. Vui lòng bắt đầu phiên mới.");
        }
        if (STATUS_CLOSED.equals(session.getTrangThai())) {
            throw new IllegalStateException("Phiên chat đã kết thúc.");
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm expire if needed.
    private boolean expireIfNeeded(ShopChatSession session) {
        if (!isActive(session) || session.getNgayTao() == null || LocalDateTime.now().isBefore(expiresAt(session))) {
            return false;
        }
        session.setTrangThai(STATUS_EXPIRED);
        session.setNgayDong(expiresAt(session));
        addMessage(session, "SYSTEM", "4MenStore", "Phiên chat đã hết hạn sau 30 phút.", false);
        sessionRepo.save(session);
        return true;
    }

    // Kiểm tra điều kiện và tính hợp lệ cho is active.
    private boolean isActive(ShopChatSession session) {
        return STATUS_AI.equals(session.getTrangThai())
                || STATUS_WAITING.equals(session.getTrangThai())
                || STATUS_STAFF.equals(session.getTrangThai());
    }

    // Thực hiện xử lý nghiệp vụ của hàm expires at.
    private LocalDateTime expiresAt(ShopChatSession session) {
        return session.getNgayTao().plus(SESSION_DURATION);
    }

    // Thực hiện xử lý nghiệp vụ của hàm apply customer.
    private void applyCustomer(ShopChatSession session, KhachHang customer) {
        session.setIdKhachHang(customer.getId());
        session.setTenKhachHang(customer.getTenKhachHang());
        session.setEmail(customer.getEmail());
    }

    // Thực hiện xử lý nghiệp vụ của hàm chat key.
    private String chatKey(HttpSession session) {
        Object existing = session.getAttribute("SHOP_CHAT_SESSION_KEY");
        if (existing != null && !String.valueOf(existing).isBlank()) return String.valueOf(existing);
        String key = SESSION_PREFIX + session.getId();
        session.setAttribute("SHOP_CHAT_SESSION_KEY", key);
        return key;
    }

    // Hiển thị và đồng bộ giao diện cho display customer name.
    private String displayCustomerName(ShopChatSession session) {
        return session.getTenKhachHang() == null || session.getTenKhachHang().isBlank()
                ? "Khách online"
                : session.getTenKhachHang();
    }

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho add message.
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

    // Thực hiện xử lý nghiệp vụ của hàm require session.
    private ShopChatSession requireSession(Integer id) {
        return sessionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên chat"));
    }

    // Thực hiện xử lý nghiệp vụ của hàm to dto.
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
                .expiresAt(session.getNgayTao() == null ? null : expiresAt(session))
                .messages(messages)
                .build();
    }

    // Thực hiện xử lý nghiệp vụ của hàm to message dto.
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
