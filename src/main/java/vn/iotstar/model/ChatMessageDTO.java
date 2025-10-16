package vn.iotstar.model;

import lombok.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private Integer maTinNhan;
    private Integer maNguoiGui;
    private String tenNguoiGui;
    private String avatarNguoiGui;
    private Integer maNguoiNhan;
    private String tenNguoiNhan;
    private Integer maCuaHang;
    private String tenCuaHang;
    private String noiDung;
    private Date thoiGian;
    private Boolean daDoc;
    private String fileUrl;
    private String type; // "text", "file", "image"
}
