package vn.iotstar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.TinNhan;

import java.util.List;

@Repository
public interface TinNhanRepository extends JpaRepository<TinNhan, Integer> {
    
    // Lấy lịch sử chat giữa 2 người trong 1 cửa hàng
    @Query("SELECT t FROM TinNhan t WHERE t.cuaHang = :cuaHang " +
           "AND ((t.nguoiGui = :nguoi1 AND t.nguoiNhan = :nguoi2) " +
           "OR (t.nguoiGui = :nguoi2 AND t.nguoiNhan = :nguoi1)) " +
           "ORDER BY t.thoiGian ASC")
    List<TinNhan> findChatHistory(@Param("cuaHang") CuaHang cuaHang,
                                   @Param("nguoi1") NguoiDung nguoi1,
                                   @Param("nguoi2") NguoiDung nguoi2);
    
    // Lấy danh sách người đã chat với user (cho cửa hàng)
    @Query("SELECT DISTINCT t.nguoiNhan FROM TinNhan t WHERE t.cuaHang = :cuaHang " +
           "AND t.nguoiGui = :nguoiDung")
    List<NguoiDung> findChatPartnersAsReceiver(@Param("cuaHang") CuaHang cuaHang,
                                                @Param("nguoiDung") NguoiDung nguoiDung);
    
    @Query("SELECT DISTINCT t.nguoiGui FROM TinNhan t WHERE t.cuaHang = :cuaHang " +
           "AND t.nguoiNhan = :nguoiDung")
    List<NguoiDung> findChatPartnersAsSender(@Param("cuaHang") CuaHang cuaHang,
                                              @Param("nguoiDung") NguoiDung nguoiDung);
    
    // Đếm tin nhắn chưa đọc
    @Query("SELECT COUNT(t) FROM TinNhan t WHERE t.nguoiNhan = :nguoiNhan " +
           "AND t.cuaHang = :cuaHang AND t.daDoc = false")
    Long countUnreadMessages(@Param("nguoiNhan") NguoiDung nguoiNhan,
                             @Param("cuaHang") CuaHang cuaHang);
    
    // Lấy tin nhắn mới nhất giữa 2 người
    @Query("SELECT t FROM TinNhan t WHERE t.cuaHang = :cuaHang " +
           "AND ((t.nguoiGui = :nguoi1 AND t.nguoiNhan = :nguoi2) " +
           "OR (t.nguoiGui = :nguoi2 AND t.nguoiNhan = :nguoi1)) " +
           "ORDER BY t.thoiGian DESC LIMIT 1")
    TinNhan findLatestMessage(@Param("cuaHang") CuaHang cuaHang,
                               @Param("nguoi1") NguoiDung nguoi1,
                               @Param("nguoi2") NguoiDung nguoi2);
    
    // Đánh dấu tất cả tin nhắn là đã đọc
    @Modifying
    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.nguoiNhan = :nguoiNhan " +
           "AND t.nguoiGui = :nguoiGui AND t.cuaHang = :cuaHang AND t.daDoc = false")
    void markAllAsRead(@Param("nguoiNhan") NguoiDung nguoiNhan,
                       @Param("nguoiGui") NguoiDung nguoiGui,
                       @Param("cuaHang") CuaHang cuaHang);
}