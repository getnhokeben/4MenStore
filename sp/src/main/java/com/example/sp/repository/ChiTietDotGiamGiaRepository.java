package com.example.sp.repository;

import com.example.sp.model.promotion.ChiTietDotGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietDotGiamGiaRepository extends JpaRepository<ChiTietDotGiamGia, Integer> {

    @Modifying
    @Query("DELETE FROM ChiTietDotGiamGia c WHERE c.dotGiamGia.id = :idDotGiamGia")
    void deleteByDotGiamGiaId(@Param("idDotGiamGia") Integer idDotGiamGia);

    @Query("SELECT c.chiTietSanPham.idSpct FROM ChiTietDotGiamGia c WHERE c.dotGiamGia.id = :idDotGiamGia")
    List<Integer> findIdSpctByDotGiamGiaId(@Param("idDotGiamGia") Integer idDotGiamGia);
}
