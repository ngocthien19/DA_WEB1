package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GioHangModel {
    private Integer maGioHang;
    
    @NotNull(message = "Người dùng không được để trống")
    private Integer maNguoiDung;
    
    private Date ngayTao;
    
    // For response
    private String tenNguoiDung;
    private List<MatHangModel> matHangs;
}