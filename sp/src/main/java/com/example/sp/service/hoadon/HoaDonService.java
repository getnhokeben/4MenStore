package com.example.sp.service.hoadon;

import com.example.sp.dto.hoadon.HoaDonChiTietDTO;
import com.example.sp.dto.hoadon.XacNhanHoanHangRequest;
import com.example.sp.dto.hoadon.XuLyGiaoHangThatBaiRequest;
import com.example.sp.model.hoadon.HoaDon;
import com.example.sp.model.hoadon.LichSuThanhToan;
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
            BigDecimal maxGia,
            Pageable pageable
    );

    BigDecimal getGiaMax();

    HoaDon taoHoaDon(Integer idKh, Integer idNv);
    void themSanPham(Integer idHoaDon, Integer idSpct, Integer soLuong);
    void xoaSanPham(Integer idHdct);
    void capNhatSoLuong(Integer idHdct, Integer soLuong);
    BigDecimal tinhTongTien(Integer idHoaDon);
    HoaDon apVoucher(Integer idHoaDon, Integer idVoucher);
    HoaDon thanhToan(Integer idHoaDon, String hinhThucThanhToan);
    HoaDon huyHoaDon(Integer idHoaDon);
    HoaDon yeuCauHoanHang(Integer idHoaDon, String lyDo);
    HoaDon xuLyGiaoHangThatBai(Integer idHoaDon, XuLyGiaoHangThatBaiRequest request);
    HoaDon xacNhanHangHoan(Integer idHoaDon, XacNhanHoanHangRequest request);
    HoaDon findById(Integer id);
    List<HoaDonChiTietDTO> getChiTiet(Integer idHoaDon);
    List<LichSuThanhToan> getLichSu(Integer idHoaDon);
    boolean updateThongTinKhachHang(Integer id, String tenKhachHang, String soDienThoai);
    HoaDon capNhatTrangThai(Integer idHoaDon, String trangThai, Integer idNhanVienThucHien);
}
