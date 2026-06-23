package com.example.sp.model;

import jakarta.persistence.*;
import lombok.Data;



@Entity
@Table(name = "chat_lieu")
@Data
public class ChatLieu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_chat_lieu")
    private Integer idChatLieu;
    @Column(name = "ma_chat_lieu")
    private String maChatLieu;
    @Column(name = "ten_chat_lieu")
    private String tenChatLieu;
    @Column(name = "trang_thai")
    private Boolean trangThai = true;
}
