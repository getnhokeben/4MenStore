package com.example.sp.repository.cuahang;

import com.example.sp.model.cuahang.ShopChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopChatMessageRepository extends JpaRepository<ShopChatMessage, Integer> {
    List<ShopChatMessage> findByIdSessionOrderByNgayTaoAscIdAsc(Integer idSession);
}
