package com.example.sp.service.cuahang;

import com.example.sp.dto.cuahang.ShopLiveChatDTO;
import com.example.sp.model.cuahang.ShopChatMessage;
import com.example.sp.model.cuahang.ShopChatSession;
import com.example.sp.repository.cuahang.ShopChatMessageRepository;
import com.example.sp.repository.cuahang.ShopChatSessionRepository;
import com.example.sp.repository.khachhang.KhachHangRepository;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopLiveChatServiceTest {

    @Test
    void expiresAnOpenSessionAfterThirtyMinutes() {
        ShopChatSessionRepository sessionRepository = mock(ShopChatSessionRepository.class);
        ShopChatMessageRepository messageRepository = mock(ShopChatMessageRepository.class);
        ShopLiveChatService service = new ShopLiveChatService(
                sessionRepository,
                messageRepository,
                mock(KhachHangRepository.class),
                mock(NhanVienRepository.class)
        );
        HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getId()).thenReturn("browser-session");

        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(31);
        ShopChatSession chatSession = ShopChatSession.builder()
                .id(18)
                .sessionKey("SHOP_CHAT_browser-session")
                .trangThai("WAITING_STAFF")
                .ngayTao(startedAt)
                .ngayCapNhat(startedAt)
                .build();
        when(sessionRepository.findBySessionKey("SHOP_CHAT_browser-session"))
                .thenReturn(Optional.of(chatSession));
        when(sessionRepository.save(any(ShopChatSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findByIdSessionOrderByNgayTaoAscIdAsc(18))
                .thenReturn(java.util.List.of());

        ShopLiveChatDTO result = service.currentCustomerSession(httpSession);

        assertEquals("EXPIRED", result.getStatus());
        assertEquals(startedAt.plusMinutes(30), result.getExpiresAt());
        verify(sessionRepository).save(chatSession);
        verify(messageRepository).save(any(ShopChatMessage.class));
    }
}
