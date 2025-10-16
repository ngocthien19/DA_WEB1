// ChatWebSocketController.java - FIXED VERSION
package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.TinNhan;
import vn.iotstar.model.ChatMessageDTO;
import vn.iotstar.service.ChatService;
import vn.iotstar.service.CuaHangService;
import vn.iotstar.service.NguoiDungService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ChatWebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private NguoiDungService nguoiDungService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO chatMessage) {
        try {
            System.out.println("📨 Received message via WebSocket: " + chatMessage);
            
            // Lấy thông tin người gửi và người nhận
            NguoiDung nguoiGui = nguoiDungService.findByMaNguoiDung(chatMessage.getMaNguoiGui());
            NguoiDung nguoiNhan = nguoiDungService.findByMaNguoiDung(chatMessage.getMaNguoiNhan());
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(chatMessage.getMaCuaHang());
            
            if (nguoiGui == null || nguoiNhan == null || cuaHang == null) {
                throw new RuntimeException("Không tìm thấy thông tin người dùng hoặc cửa hàng");
            }
            
            // Tạo tin nhắn mới
            TinNhan tinNhan = TinNhan.builder()
                    .nguoiGui(nguoiGui)
                    .nguoiNhan(nguoiNhan)
                    .cuaHang(cuaHang)
                    .noiDung(chatMessage.getNoiDung())
                    .thoiGian(new Date())
                    .daDoc(false)
                    .fileUrl(chatMessage.getFileUrl())
                    .build();
            
            // Lưu vào database
            TinNhan savedMessage = chatService.saveMessage(tinNhan);
            
            // Convert sang DTO
            ChatMessageDTO responseMessage = chatService.convertToDTO(savedMessage);
            responseMessage.setTenNguoiGui(nguoiGui.getTenNguoiDung()); // Thêm tên người gửi
            
            System.out.println("💾 Message saved to DB: " + savedMessage.getMaTinNhan());
            System.out.println("📤 Sending to receiver: " + nguoiNhan.getMaNguoiDung());
            System.out.println("📩 Sending to sender: " + nguoiGui.getMaNguoiDung());
            
            // Gửi tin nhắn đến người nhân
            messagingTemplate.convertAndSendToUser(
                    nguoiNhan.getMaNguoiDung().toString(),
                    "/queue/messages",
                    responseMessage
            );
            
            // Gửi lại cho người gửi để xác nhận (đảm bảo hiển thị ngay lập tức)
            messagingTemplate.convertAndSendToUser(
                    nguoiGui.getMaNguoiDung().toString(),
                    "/queue/messages", 
                    responseMessage
            );
            
            System.out.println("✅ Message sent successfully to both parties");
            
        } catch (Exception e) {
            e.printStackTrace();
            // Gửi thông báo lỗi
            Map<String, String> error = new HashMap<>();
            error.put("error", "Không thể gửi tin nhắn: " + e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getMaNguoiGui().toString(),
                    "/queue/errors",
                    error
            );
        }
    }
    
    @MessageMapping("/chat.typing")
    public void userTyping(@Payload Map<String, Object> typingInfo) {
        try {
            Integer nguoiNhanId = (Integer) typingInfo.get("nguoiNhanId");
            Integer nguoiGuiId = (Integer) typingInfo.get("nguoiGuiId");
            Boolean isTyping = (Boolean) typingInfo.get("isTyping");
            
            System.out.println("⌨️ Typing event: " + nguoiGuiId + " -> " + nguoiNhanId + " = " + isTyping);
            
            // Gửi thông báo đang gõ đến người nhận
            messagingTemplate.convertAndSendToUser(
                    nguoiNhanId.toString(),
                    "/queue/typing",
                    typingInfo
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}