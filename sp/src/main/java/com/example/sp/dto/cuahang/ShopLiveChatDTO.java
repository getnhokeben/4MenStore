package com.example.sp.dto.cuahang;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ShopLiveChatDTO {
    private Integer id;
    private String status;
    private String customerName;
    private String customerEmail;
    private String employeeName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private List<Message> messages;

    @Data
    @Builder
    public static class Message {
        private Integer id;
        private String senderType;
        private String senderName;
        private String content;
        private Boolean aiGenerated;
        private LocalDateTime createdAt;
    }
}
