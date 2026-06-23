package com.example.sp.repository;

import com.example.sp.model.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {
    boolean existsByMaSp(String maSp);
    List<SanPham> findSanPhamsByTenSpContains(String ten);

    @Query("SELECT s FROM SanPham s WHERE " +
            "(:keyword IS NULL OR s.maSp LIKE %:keyword% OR s.tenSp LIKE %:keyword%) AND " +
            "(:chatLieu IS NULL OR s.chatLieu.tenChatLieu = :chatLieu) AND " +
            "(:trangThai IS NULL OR s.trangThai = :trangThai)")
    Page<SanPham> findByFilters(@Param("keyword") String keyword,
                                @Param("chatLieu") String chatLieu,
                                @Param("trangThai") Boolean trangThai,
                                Pageable pageable);
}