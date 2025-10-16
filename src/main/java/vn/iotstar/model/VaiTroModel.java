package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaiTroModel {
    private String maVaiTro;
    
    @NotBlank(message = "Tên vai trò không được để trống")
    private String tenVaiTro;
}