package com.example.sp.service.sanpham;

import com.example.sp.model.sanpham.LoaiAo;
import com.example.sp.repository.sanpham.LoaiAoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class LoaiAoService {

    @Autowired private LoaiAoRepository repository;

    // Tải hoặc truy xuất dữ liệu cho find all.
    public Page<LoaiAo> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    // Thực hiện xử lý nghiệp vụ của hàm search.
    public Page<LoaiAo> search(String keyword, Pageable pageable) {
        return repository.findByTenLoaiContainingIgnoreCaseOrMaLoaiContainingIgnoreCase(keyword, keyword, pageable);
    }

    // Tải hoặc truy xuất dữ liệu cho find by id.
    public Optional<LoaiAo> findById(Integer id) {
        return repository.findById(id);
    }

    // Tạo hoặc cập nhật dữ liệu/trạng thái cho save.
    public LoaiAo save(LoaiAo entity) {
        return repository.save(entity);
    }

    // Xử lý thao tác đóng, xóa hoặc hủy cho delete by id.
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }
}
