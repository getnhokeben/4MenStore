package com.example.sp.service.order;

import com.example.sp.dto.HoaDonChiTietDTO;
import com.example.sp.model.order.HoaDon;
import com.example.sp.model.order.LichSuThanhToan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
public interface HoaDonService {

    List<HoaDon> findAll();

    Page<HoaDon> timKiem(
            String maHD,
            String tuNgay,
            String denNgay,
            String loaiDon,
            String trangThai,
            Pageable pageable
    );

    HoaDon taoHoaDon(Integer idKh, Integer idNv);

    void themSanPham(Integer idHoaDon, Integer idSpct, Integer soLuong);

    void xoaSanPham(Integer idHdct);

    void capNhatSoLuong(Integer idHdct, Integer soLuong);

    BigDecimal tinhTongTien(Integer idHoaDon);

    HoaDon apVoucher(Integer idHoaDon, Integer idVoucher);

    HoaDon thanhToan(Integer idHoaDon, String hinhThucThanhToan);

    HoaDon huyHoaDon(Integer idHoaDon);

    HoaDon findById(Integer id);

    List<HoaDonChiTietDTO> getChiTiet(Integer idHoaDon);

    List<LichSuThanhToan> getLichSu(Integer idHoaDon);

    boolean updateThongTinKhachHang(Integer id, String tenKhachHang, String soDienThoai);

    HoaDon capNhatTrangThai(Integer idHoaDon, String trangThai);
}