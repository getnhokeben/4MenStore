package com.example.sp.service;

import com.example.sp.model.LoaiAo;
import com.example.sp.repository.LoaiAoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class LoaiAoService {

    @Autowired private LoaiAoRepository repository;

    public Page<LoaiAo> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<LoaiAo> search(String keyword, Pageable pageable) {
        return repository.findByTenLoaiContainingIgnoreCaseOrMaLoaiContainingIgnoreCase(keyword, keyword, pageable);
    }

    public Optional<LoaiAo> findById(Integer id) {
        return repository.findById(id);
    }

    public LoaiAo save(LoaiAo entity) {
        return repository.save(entity);
    }

    public void deleteById(Integer id) {
        repository.deleteById(id);
    }
}