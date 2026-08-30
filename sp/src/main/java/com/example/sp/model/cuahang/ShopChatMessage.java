package com.example.sp.model.cuahang;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shop_chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_message")
    private Integer id;

    @Column(name = "id_session", nullable = false)
    private Integer idSession;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;

    @Column(name = "sender_name", length = 255)
    private String senderName;

    @Column(name = "noi_dung", nullable = false, length = 1200)
    private String noiDung;

    @Column(name = "ai_generated")
    private Boolean aiGenerated;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    // Thực hiện xử lý nghiệp vụ của hàm pre persist.
    public void prePersist() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
        if (aiGenerated == null) aiGenerated = false;
    }
}
