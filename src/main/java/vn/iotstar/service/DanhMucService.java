package vn.iotstar.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.iotstar.entity.DanhMuc;

import java.util.List;

public interface DanhMucService {
    
    List<DanhMuc> findAll();
    
    Page<DanhMuc> findAll(Pageable pageable);
    
    Page<DanhMuc> searchCategories(String keyword, Pageable pageable);
    
    DanhMuc findByMaDanhMuc(Integer maDanhMuc);
    
    DanhMuc findByTenDanhMuc(String tenDanhMuc);
    
    DanhMuc save(DanhMuc danhMuc);
    
    DanhMuc createCategory(DanhMuc danhMuc);
    
    DanhMuc updateCategory(Integer id, DanhMuc danhMuc);
    
    void deleteCategory(Integer id);
    
    List<DanhMuc> findAllActiveCategories();
    
    long countActiveCategories();
}