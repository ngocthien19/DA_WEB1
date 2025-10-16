package vn.iotstar.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.iotstar.entity.NguoiDung;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class NguoiDungSpecification {

    /**
     * Lọc người dùng theo từ khóa tìm kiếm và vai trò
     */
    public static Specification<NguoiDung> filterUsers(String keyword, String maVaiTro) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tìm kiếm theo tên hoặc email
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchPattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("tenNguoiDung")),
                    searchPattern
                );
                Predicate emailPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("email")),
                    searchPattern
                );
                predicates.add(criteriaBuilder.or(namePredicate, emailPredicate));
            }

            // Lọc theo vai trò
            if (maVaiTro != null && !maVaiTro.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    root.get("vaiTro").get("maVaiTro"), 
                    maVaiTro
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Tìm kiếm người dùng theo tên
     */
    public static Specification<NguoiDung> hasTenNguoiDungContaining(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("tenNguoiDung")),
                "%" + keyword.toLowerCase() + "%"
            );
        };
    }

    /**
     * Tìm kiếm người dùng theo email
     */
    public static Specification<NguoiDung> hasEmailContaining(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("email")),
                "%" + keyword.toLowerCase() + "%"
            );
        };
    }

    /**
     * Lọc người dùng theo vai trò
     */
    public static Specification<NguoiDung> hasVaiTro(String maVaiTro) {
        return (root, query, criteriaBuilder) -> {
            if (maVaiTro == null || maVaiTro.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("vaiTro").get("maVaiTro"), maVaiTro);
        };
    }

    /**
     * Lọc người dùng theo trạng thái
     */
    public static Specification<NguoiDung> hasTrangThai(String trangThai) {
        return (root, query, criteriaBuilder) -> {
            if (trangThai == null || trangThai.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("trangThai"), trangThai);
        };
    }
}