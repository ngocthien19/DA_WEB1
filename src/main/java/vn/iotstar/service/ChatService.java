package vn.iotstar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.TinNhan;
import vn.iotstar.model.ChatMessageDTO;
import vn.iotstar.repository.TinNhanRepository;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {
    
    @Autowired
    private TinNhanRepository tinNhanRepository;
    
    @Transactional
    public TinNhan saveMessage(TinNhan tinNhan) {
        return tinNhanRepository.save(tinNhan);
    }
    
    public List<TinNhan> getChatHistory(CuaHang cuaHang, NguoiDung nguoi1, NguoiDung nguoi2) {
        return tinNhanRepository.findChatHistory(cuaHang, nguoi1, nguoi2);
    }
    
    public List<NguoiDung> getChatPartners(CuaHang cuaHang, NguoiDung nguoiDung) {
        // Lấy danh sách người nhận (người mà user đã gửi tin nhắn)
        List<NguoiDung> receivers = tinNhanRepository.findChatPartnersAsReceiver(cuaHang, nguoiDung);
        
        // Lấy danh sách người gửi (người đã gửi tin nhắn cho user)
        List<NguoiDung> senders = tinNhanRepository.findChatPartnersAsSender(cuaHang, nguoiDung);
        
        // Merge 2 danh sách và loại bỏ duplicate
        Set<NguoiDung> uniquePartners = new java.util.HashSet<>();
        uniquePartners.addAll(receivers);
        uniquePartners.addAll(senders);
        
        return new java.util.ArrayList<>(uniquePartners);
    }
    
    public Long countUnreadMessages(NguoiDung nguoiNhan, CuaHang cuaHang) {
        return tinNhanRepository.countUnreadMessages(nguoiNhan, cuaHang);
    }
    
    @Transactional
    public void markAsRead(NguoiDung nguoiNhan, NguoiDung nguoiGui, CuaHang cuaHang) {
        tinNhanRepository.markAllAsRead(nguoiNhan, nguoiGui, cuaHang);
    }
    
    public TinNhan getLatestMessage(CuaHang cuaHang, NguoiDung nguoi1, NguoiDung nguoi2) {
        return tinNhanRepository.findLatestMessage(cuaHang, nguoi1, nguoi2);
    }
    
    // THÊM CÁC METHOD MỚI Ở ĐÂY
    
    public List<CuaHang> getStoresWithChatHistory(NguoiDung nguoiDung) {
        return tinNhanRepository.findDistinctStoresByNguoiDung(nguoiDung);
    }
    
    public TinNhan getLatestMessageWithStore(NguoiDung nguoiDung, CuaHang cuaHang) {
        return tinNhanRepository.findTopByNguoiNhanAndCuaHangOrNguoiGuiAndCuaHangOrderByThoiGianDesc(
            nguoiDung, cuaHang, nguoiDung, cuaHang);
    }
    
    public Long countTotalUnreadMessages(NguoiDung nguoiNhan) {
        return tinNhanRepository.countByNguoiNhanAndDaDocFalse(nguoiNhan);
    }
    
    public Long countUnreadMessagesFromSender(NguoiDung nguoiNhan, NguoiDung nguoiGui) {
        return tinNhanRepository.countByNguoiNhanAndNguoiGuiAndDaDocFalse(nguoiNhan, nguoiGui);
    }
    
    public Long countUnreadMessagesByStore(NguoiDung nguoiNhan, CuaHang cuaHang) {
        return tinNhanRepository.countByNguoiNhanAndCuaHangAndDaDocFalse(nguoiNhan, cuaHang);
    }
    
    @Transactional
    public void markAllAsReadByStore(NguoiDung nguoiNhan, CuaHang cuaHang) {
        tinNhanRepository.markAllAsReadByStore(nguoiNhan, cuaHang);
    }
    
    public List<TinNhan> getChatHistoryWithStore(NguoiDung nguoiDung, CuaHang cuaHang) {
        return tinNhanRepository.findByCuaHangAndNguoiNhanOrNguoiGuiOrderByThoiGianAsc(
            cuaHang, nguoiDung, nguoiDung);
    }
    
    // Convert Entity to DTO
    public ChatMessageDTO convertToDTO(TinNhan tinNhan) {
        return ChatMessageDTO.builder()
                .maTinNhan(tinNhan.getMaTinNhan())
                .maNguoiGui(tinNhan.getNguoiGui().getMaNguoiDung())
                .tenNguoiGui(tinNhan.getNguoiGui().getTenNguoiDung())
                .avatarNguoiGui(tinNhan.getNguoiGui().getHinhAnh())
                .maNguoiNhan(tinNhan.getNguoiNhan().getMaNguoiDung())
                .tenNguoiNhan(tinNhan.getNguoiNhan().getTenNguoiDung())
                .maCuaHang(tinNhan.getCuaHang().getMaCuaHang())
                .tenCuaHang(tinNhan.getCuaHang().getTenCuaHang())
                .noiDung(tinNhan.getNoiDung())
                .thoiGian(tinNhan.getThoiGian())
                .daDoc(tinNhan.getDaDoc())
                .fileUrl(tinNhan.getFileUrl())
                .type(tinNhan.getFileUrl() != null ? "file" : "text")
                .build();
    }
    
    public List<ChatMessageDTO> convertToDTOList(List<TinNhan> tinNhans) {
        return tinNhans.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}