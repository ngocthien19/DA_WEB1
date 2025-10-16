package vn.iotstar.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "VaiTro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaiTro {
    @Id
    @NotBlank(message = "Mã vai trò không được để trống")
    @Column(name = "MaVaiTro", columnDefinition = "VARCHAR(50)")
    private String maVaiTro;
    
    @NotBlank(message = "Tên vai trò không được để trống")
    @Column(name = "TenVaiTro", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String tenVaiTro;
}