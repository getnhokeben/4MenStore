package com.example.sp.repository;

import com.example.sp.model.SanPham;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

public class SanPhamSpecification {
    public static Specification<SanPham> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) return cb.conjunction();
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("maSp")), likePattern),
                    cb.like(cb.lower(root.get("tenSp")), likePattern)
            );
        };
    }

    public static Specification<SanPham> hasChatLieu(String chatLieu) {
        return (root, query, cb) -> {
            if (chatLieu == null || chatLieu.isEmpty()) return cb.conjunction();
            return cb.equal(root.get("chatLieu").get("tenChatLieu"), chatLieu);
        };
    }

    public static Specification<SanPham> hasThuongHieu(String thuongHieu) {
        // Giả sử SanPham có trường thuongHieu (có thể thêm vào entity)
        return (root, query, cb) -> {
            if (thuongHieu == null || thuongHieu.isEmpty()) return cb.conjunction();
            return cb.equal(root.get("thuongHieu"), thuongHieu);
        };
    }

    public static Specification<SanPham> hasTrangThai(Boolean trangThai) {
        return (root, query, cb) -> {
            if (trangThai == null) return cb.conjunction();
            return cb.equal(root.get("trangThai"), trangThai);
        };
    }
}
