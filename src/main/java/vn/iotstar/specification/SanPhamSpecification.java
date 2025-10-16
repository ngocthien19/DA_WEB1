package vn.iotstar.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.SanPham;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SanPhamSpecification {

	public static Specification<SanPham> filterProducts(
            DanhMuc danhMuc,
            List<String> prices,
            List<String> stores,
            List<String> loais,
            List<String> stars,
            String search) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // THÊM ĐIỀU KIỆN: CHỈ lấy sản phẩm đang hoạt động
            predicates.add(criteriaBuilder.isTrue(root.get("trangThai")));
            
            // THÊM ĐIỀU KIỆN: CHỈ lấy sản phẩm từ cửa hàng đang hoạt động
            predicates.add(criteriaBuilder.isTrue(root.get("cuaHang").get("trangThai")));
            
            // THÊM ĐIỀU KIỆN: CHỈ lấy sản phẩm từ danh mục đang hoạt động
            predicates.add(criteriaBuilder.isTrue(root.get("danhMuc").get("trangThai")));
            
            // Filter by category
            if (danhMuc != null) {
                predicates.add(criteriaBuilder.equal(root.get("danhMuc"), danhMuc));
            }

            // Filter by price ranges
            if (prices != null && !prices.isEmpty()) {
                List<Predicate> pricePredicates = new ArrayList<>();
                for (String range : prices) {
                    switch (range) {
                        case "below10":
                            pricePredicates.add(criteriaBuilder.lessThan(
                                root.get("giaBan"), new BigDecimal("10000000")));
                            break;
                        case "1015":
                            pricePredicates.add(criteriaBuilder.between(
                                root.get("giaBan"), 
                                new BigDecimal("10000000"), 
                                new BigDecimal("15000000")));
                            break;
                        case "1520":
                            pricePredicates.add(criteriaBuilder.between(
                                root.get("giaBan"), 
                                new BigDecimal("15000000"), 
                                new BigDecimal("20000000")));
                            break;
                        case "2025":
                            pricePredicates.add(criteriaBuilder.between(
                                root.get("giaBan"), 
                                new BigDecimal("20000000"), 
                                new BigDecimal("25000000")));
                            break;
                        case "above25":
                            pricePredicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("giaBan"), new BigDecimal("25000000")));
                            break;
                    }
                }
                predicates.add(criteriaBuilder.or(pricePredicates.toArray(new Predicate[0])));
            }

            // Filter by stores
            if (stores != null && !stores.isEmpty()) {
                predicates.add(root.get("cuaHang").get("tenCuaHang").in(stores));
            }

            // Filter by product types
            if (loais != null && !loais.isEmpty()) {
                predicates.add(root.get("loaiSanPham").in(loais));
            }

            // Filter by star ratings (Integer)
            if (stars != null && !stars.isEmpty()) {
                List<Predicate> starPredicates = new ArrayList<>();
                for (String s : stars) {
                    switch (s) {
                        case "0":
                            starPredicates.add(criteriaBuilder.or(
                                criteriaBuilder.isNull(root.get("saoDanhGia")),
                                criteriaBuilder.equal(root.get("saoDanhGia"), 0)
                            ));
                            break;
                        case "12":
                            starPredicates.add(criteriaBuilder.between(
                                root.get("saoDanhGia"), 1, 2));
                            break;
                        case "23":
                            starPredicates.add(criteriaBuilder.equal(
                                root.get("saoDanhGia"), 3));
                            break;
                        case "34":
                            starPredicates.add(criteriaBuilder.equal(
                                root.get("saoDanhGia"), 4));
                            break;
                        case "45":
                            starPredicates.add(criteriaBuilder.equal(
                                root.get("saoDanhGia"), 5));
                            break;
                    }
                }
                predicates.add(criteriaBuilder.or(starPredicates.toArray(new Predicate[0])));
            }

            // Search by product name
            if (search != null && !search.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("tenSanPham")),
                    "%" + search.toLowerCase().trim() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification mới cho trang tất cả sản phẩm - chỉ lấy từ danh mục đang hoạt động
     * Được sử dụng trong phương thức allProducts() của HomeController
     */
    public static Specification<SanPham> filterProductsWithActiveCategories(
            DanhMuc danhMuc,
            List<String> prices,
            List<String> stores,
            List<String> loais,
            List<String> stars,
            String search) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // THÊM ĐIỀU KIỆN: CHỈ lấy sản phẩm đang hoạt động
            predicates.add(criteriaBuilder.isTrue(root.get("trangThai")));
            
            // THÊM ĐIỀU KIỆN: CHỈ lấy sản phẩm từ cửa hàng đang hoạt động
            predicates.add(criteriaBuilder.isTrue(root.get("cuaHang").get("trangThai")));
            
            // THÊM ĐIỀU KIỆN: CHỈ lấy sản phẩm từ danh mục đang hoạt động
            predicates.add(criteriaBuilder.isTrue(root.get("danhMuc").get("trangThai")));
            
            // Filter by category (nếu có)
            if (danhMuc != null) {
                predicates.add(criteriaBuilder.equal(root.get("danhMuc"), danhMuc));
            }

            // Filter by price ranges
            if (prices != null && !prices.isEmpty()) {
                List<Predicate> pricePredicates = new ArrayList<>();
                for (String range : prices) {
                    switch (range) {
                        case "below10":
                            pricePredicates.add(criteriaBuilder.lessThan(
                                root.get("giaBan"), new BigDecimal("10000000")));
                            break;
                        case "1015":
                            pricePredicates.add(criteriaBuilder.between(
                                root.get("giaBan"), 
                                new BigDecimal("10000000"), 
                                new BigDecimal("15000000")));
                            break;
                        case "1520":
                            pricePredicates.add(criteriaBuilder.between(
                                root.get("giaBan"), 
                                new BigDecimal("15000000"), 
                                new BigDecimal("20000000")));
                            break;
                        case "2025":
                            pricePredicates.add(criteriaBuilder.between(
                                root.get("giaBan"), 
                                new BigDecimal("20000000"), 
                                new BigDecimal("25000000")));
                            break;
                        case "above25":
                            pricePredicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("giaBan"), new BigDecimal("25000000")));
                            break;
                    }
                }
                predicates.add(criteriaBuilder.or(pricePredicates.toArray(new Predicate[0])));
            }

            // Filter by stores
            if (stores != null && !stores.isEmpty()) {
                predicates.add(root.get("cuaHang").get("tenCuaHang").in(stores));
            }

            // Filter by product types
            if (loais != null && !loais.isEmpty()) {
                predicates.add(root.get("loaiSanPham").in(loais));
            }

            // Filter by star ratings (Integer)
            if (stars != null && !stars.isEmpty()) {
                List<Predicate> starPredicates = new ArrayList<>();
                for (String s : stars) {
                    switch (s) {
                        case "0":
                            starPredicates.add(criteriaBuilder.or(
                                criteriaBuilder.isNull(root.get("saoDanhGia")),
                                criteriaBuilder.equal(root.get("saoDanhGia"), 0)
                            ));
                            break;
                        case "12":
                            starPredicates.add(criteriaBuilder.between(
                                root.get("saoDanhGia"), 1, 2));
                            break;
                        case "23":
                            starPredicates.add(criteriaBuilder.equal(
                                root.get("saoDanhGia"), 3));
                            break;
                        case "34":
                            starPredicates.add(criteriaBuilder.equal(
                                root.get("saoDanhGia"), 4));
                            break;
                        case "45":
                            starPredicates.add(criteriaBuilder.equal(
                                root.get("saoDanhGia"), 5));
                            break;
                    }
                }
                predicates.add(criteriaBuilder.or(starPredicates.toArray(new Predicate[0])));
            }

            // Search by product name
            if (search != null && !search.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("tenSanPham")),
                    "%" + search.toLowerCase().trim() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification cho tìm kiếm sản phẩm theo tên (cho admin hoặc tìm kiếm nâng cao)
     */
    public static Specification<SanPham> hasTenSanPhamContaining(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("tenSanPham")),
                "%" + keyword.toLowerCase() + "%"
            );
        };
    }
}