package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.DanhMuc;
import vn.iotstar.entity.NguoiDung;
import vn.iotstar.entity.TinNhan;
import vn.iotstar.model.ChatMessageDTO;
import vn.iotstar.service.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/chat")
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private NguoiDungService nguoiDungService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private DanhMucService danhMucService;
    
    // Hiển thị trang chat
    @GetMapping("/store/{storeId}")
    public String showChatPage(@PathVariable Integer storeId, Model model) {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return "redirect:/login";
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            CuaHang cuaHang = cuaHangService.findByMaCuaHang(storeId);
            if (cuaHang == null) {
                model.addAttribute("error", "Không tìm thấy cửa hàng");
                return "redirect:/stores";
            }

            // Thêm danh mục vào model (cho header)
            List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
            model.addAttribute("danhMucs", danhMucs);
            model.addAttribute("nguoiDung", nguoiDung);
            model.addAttribute("cuaHang", cuaHang);
            model.addAttribute("vendor", cuaHang.getNguoiDung());

            return "web/chat";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/stores";
        }
    }
    
    // Lấy lịch sử chat
    @GetMapping("/history/{storeId}/{partnerId}")
    @ResponseBody
    public ResponseEntity<?> getChatHistory(@PathVariable Integer storeId, @PathVariable Integer partnerId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            
            NguoiDung partner = nguoiDungService.findByMaNguoiDung(partnerId);
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(storeId);

            List<TinNhan> messages = chatService.getChatHistory(cuaHang, nguoiDung, partner);
            List<ChatMessageDTO> messageDTOs = chatService.convertToDTOList(messages);
            
            // Đánh dấu đã đọc
            chatService.markAsRead(nguoiDung, partner, cuaHang);

            return ResponseEntity.ok(messageDTOs);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Lấy danh sách người đã chat
    @GetMapping("/partners/{storeId}")
    @ResponseBody
    public ResponseEntity<?> getChatPartners(@PathVariable Integer storeId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(storeId);
            
            List<NguoiDung> partners = chatService.getChatPartners(cuaHang, nguoiDung);
            
            // Tạo danh sách với thông tin tin nhắn cuối
            List<Map<String, Object>> partnerList = partners.stream().map(partner -> {
                Map<String, Object> info = new HashMap<>();
                info.put("maNguoiDung", partner.getMaNguoiDung());
                info.put("hoTen", partner.getTenNguoiDung());
                info.put("hinhAnh", partner.getHinhAnh());
                
                TinNhan latestMessage = chatService.getLatestMessage(cuaHang, nguoiDung, partner);
                if (latestMessage != null) {
                    info.put("lastMessage", latestMessage.getNoiDung());
                    info.put("lastMessageTime", latestMessage.getThoiGian());
                }
                
                Long unreadCount = chatService.countUnreadMessages(nguoiDung, cuaHang);
                info.put("unreadCount", unreadCount);
                
                return info;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(partnerList);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Đánh dấu tin nhắn đã đọc
    @PostMapping("/mark-read/{storeId}/{partnerId}")
    @ResponseBody
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Integer storeId, @PathVariable Integer partnerId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            
            NguoiDung partner = nguoiDungService.findByMaNguoiDung(partnerId);
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(storeId);

            chatService.markAsRead(nguoiDung, partner, cuaHang);

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}