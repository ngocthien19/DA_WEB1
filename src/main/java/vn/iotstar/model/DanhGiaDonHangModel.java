package vn.iotstar.model;

import lombok.*;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGiaDonHangModel {
    
    @NotNull(message = "Mã đơn hàng không được để trống")
    private Integer maDatHang;
    
    // Đánh giá sản phẩm: key = maSanPham, value = DanhGiaSanPhamItem
    private Map<Integer, DanhGiaSanPhamItem> danhGiaSanPhams;
    
    // Đánh giá cửa hàng
    @NotNull(message = "Đánh giá cửa hàng không được để trống")
    private DanhGiaCuaHangItem danhGiaCuaHang;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DanhGiaSanPhamItem {
        @NotNull(message = "Số sao không được để trống")
        @Min(value = 1, message = "Số sao phải từ 1 đến 5")
        @Max(value = 5, message = "Số sao phải từ 1 đến 5")
        private Integer soSao;
        
        @NotBlank(message = "Bình luận không được để trống")
        @Size(min = 10, max = 1000, message = "Bình luận phải từ 10 đến 1000 ký tự")
        private String binhLuan;
        
        private List<MultipartFile> anhVideos;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DanhGiaCuaHangItem {
        @NotNull(message = "Số sao không được để trống")
        @Min(value = 1, message = "Số sao phải từ 1 đến 5")
        @Max(value = 5, message = "Số sao phải từ 1 đến 5")
        private Integer soSao;
        
        @NotBlank(message = "Bình luận không được để trống")
        @Size(min = 10, max = 1000, message = "Bình luận phải từ 10 đến 1000 ký tự")
        private String binhLuan;
    }
}
