package com.example.sp.repository.cuahang;

import com.example.sp.model.cuahang.ShopChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShopChatSessionRepository extends JpaRepository<ShopChatSession, Integer> {
    Optional<ShopChatSession> findBySessionKey(String sessionKey);

    List<ShopChatSession> findByTrangThaiInOrderByNgayCapNhatDesc(Collection<String> trangThai);
}
