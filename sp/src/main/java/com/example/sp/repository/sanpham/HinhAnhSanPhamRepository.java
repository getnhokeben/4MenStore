package com.example.sp.repository.sanpham;

import com.example.sp.model.sanpham.HinhAnhSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HinhAnhSanPhamRepository extends JpaRepository<HinhAnhSanPham, Integer> {
    List<HinhAnhSanPham> findByIdSanPham(Integer idSanPham);
    void deleteByIdSanPham(Integer idSanPham);
}
