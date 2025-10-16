package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.VaiTro;

import java.util.Optional;

@Repository
public interface VaiTroRepository extends JpaRepository<VaiTro, String> {
    Optional<VaiTro> findByTenVaiTro(String tenVaiTro);
}