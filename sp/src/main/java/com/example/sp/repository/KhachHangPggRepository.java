package com.example.sp.repository;

import com.example.sp.model.promotion.KhachHangPgg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhachHangPggRepository extends JpaRepository<KhachHangPgg, Integer> {

    @Modifying
    @Query("DELETE FROM KhachHangPgg k WHERE k.idPgg = :idPgg")
    void deleteByIdPgg(@Param("idPgg") Integer idPgg);

    @Query("SELECT k.idKh FROM KhachHangPgg k WHERE k.idPgg = :idPgg")
    List<Integer> findIdKhByIdPgg(@Param("idPgg") Integer idPgg);
}
