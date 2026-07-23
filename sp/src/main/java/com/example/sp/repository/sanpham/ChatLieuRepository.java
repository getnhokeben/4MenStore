package com.example.sp.repository.sanpham;

import com.example.sp.model.sanpham.ChatLieu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatLieuRepository extends JpaRepository<ChatLieu, Integer> {
    List<ChatLieu> findByTenChatLieu(String tenChatLieu);
}