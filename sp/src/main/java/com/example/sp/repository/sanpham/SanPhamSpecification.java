package com.example.sp.repository.sanpham;

import com.example.sp.model.sanpham.SanPham;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;

public class SanPhamSpecification {
    // Kiểm tra điều kiện và tính hợp lệ cho has keyword.
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

    // Kiểm tra điều kiện và tính hợp lệ cho has chat lieu.
    public static Specification<SanPham> hasChatLieu(String chatLieu) {
        return (root, query, cb) -> {
            if (chatLieu == null || chatLieu.isEmpty()) return cb.conjunction();
            return cb.equal(root.get("chatLieu").get("tenChatLieu"), chatLieu);
        };
    }

    // Kiểm tra điều kiện và tính hợp lệ cho has thuong hieu.
    public static Specification<SanPham> hasThuongHieu(String thuongHieu) {
        // Giả sử SanPham có trường thuongHieu (có thể thêm vào entity)
        return (root, query, cb) -> {
            if (thuongHieu == null || thuongHieu.isEmpty()) return cb.conjunction();
            return cb.equal(root.get("thuongHieu"), thuongHieu);
        };
    }

    // Kiểm tra điều kiện và tính hợp lệ cho has trang thai.
    public static Specification<SanPham> hasTrangThai(Boolean trangThai) {
        return (root, query, cb) -> {
            if (trangThai == null) return cb.conjunction();
            return cb.equal(root.get("trangThai"), trangThai);
        };
    }
}
