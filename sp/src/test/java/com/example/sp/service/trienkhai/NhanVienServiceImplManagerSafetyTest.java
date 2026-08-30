package com.example.sp.service.trienkhai;

import com.example.sp.model.nhanvien.NhanVien;
import com.example.sp.repository.nhanvien.NhanVienRepository;
import com.example.sp.service.nhanvien.EmployeeAccountMailService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NhanVienServiceImplManagerSafetyTest {

    @Test
    void cannotDisableLastActiveManager() {
        NhanVienRepository repository = mock(NhanVienRepository.class);
        NhanVien manager = activeManager();
        when(repository.findById(1)).thenReturn(Optional.of(manager));
        when(repository.existsByVaiTroAndTrangThaiTrueAndIdNot("Quản lý", 1)).thenReturn(false);

        NhanVienServiceImpl service = service(repository);

        assertThrows(IllegalArgumentException.class, () -> service.toggleStatus(1));
        verify(repository).existsByVaiTroAndTrangThaiTrueAndIdNot("Quản lý", 1);
    }

    @Test
    void cannotChangeLastActiveManagerToEmployee() {
        NhanVienRepository repository = mock(NhanVienRepository.class);
        NhanVien manager = activeManager();
        when(repository.findById(1)).thenReturn(Optional.of(manager));
        when(repository.existsByVaiTroAndTrangThaiTrueAndIdNot("Quản lý", 1)).thenReturn(false);

        NhanVien request = activeManager();
        request.setVaiTro("Nhân viên");

        NhanVienServiceImpl service = service(repository);

        assertThrows(IllegalArgumentException.class, () -> service.update(1, request));
        verify(repository).existsByVaiTroAndTrangThaiTrueAndIdNot("Quản lý", 1);
    }

    private NhanVienServiceImpl service(NhanVienRepository repository) {
        return new NhanVienServiceImpl(repository, mock(EmployeeAccountMailService.class));
    }

    private NhanVien activeManager() {
        NhanVien employee = new NhanVien();
        employee.setId(1);
        employee.setHoTen("Nguyen Van A");
        employee.setEmail("manager@example.com");
        employee.setSoDienThoai("0901234567");
        employee.setCccd("001234567890");
        employee.setGioiTinh("Nam");
        employee.setNgaySinh(LocalDate.now().minusYears(30));
        employee.setNgayVaoLam(LocalDate.now().minusYears(5));
        employee.setTinhThanh("Ha Noi");
        employee.setPhuongXa("Phuong 1");
        employee.setDiaChiChiTiet("1 Duong A");
        employee.setVaiTro("Quản lý");
        employee.setTrangThai(true);
        return employee;
    }
}
