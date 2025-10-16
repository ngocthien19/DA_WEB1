package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.DanhMuc;

import java.util.List;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, Integer> {

    // Lấy tất cả các danh mục
    List<DanhMuc> findAll();

    // Lấy danh mục theo mã danh mục
    DanhMuc findByMaDanhMuc(Integer maDanhMuc);
    
    DanhMuc findByTenDanhMuc(String tenDanhMuc);
    
    // THÊM METHOD MỚI
    long count(); // Đếm tổng số danh mục
    
    Page<DanhMuc> findByTenDanhMucContainingIgnoreCase(String tenDanhMuc, Pageable pageable);
    
    @Query("SELECT d FROM DanhMuc d WHERE d.trangThai = true ORDER BY d.ngayTao DESC")
    List<DanhMuc> findAllActiveCategories();
    
    @Query("SELECT COUNT(d) FROM DanhMuc d WHERE d.trangThai = true")
    long countActiveCategories();
}
