package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.iotstar.entity.*;
import vn.iotstar.repository.DanhGiaRepository;
import vn.iotstar.repository.DanhGiaCuaHangRepository;
import vn.iotstar.service.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
@RequestMapping("/review")
public class ReviewController {
    
    @Autowired
    private DatHangService datHangService;
    
    @Autowired
    private NguoiDungService nguoiDungService;
    
    @Autowired
    private DanhGiaRepository danhGiaRepository;
    
    @Autowired
    private DanhGiaCuaHangRepository danhGiaCuaHangRepository;
    
    @Autowired
    private DanhGiaCuaHangService danhGiaCuaHangService;
    
    @Autowired
    private CuaHangService cuaHangService;
    
    @Autowired
    private SanPhamService sanPhamService;
    
    @Autowired
    private DanhMucService danhMucService;
    
    private static final String UPLOAD_DIR = "uploads/reviews/";
    
    @GetMapping("/{orderId}")
    public String showReviewForm(@PathVariable Integer orderId, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return "redirect:/login";
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            // Tìm người dùng
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Lấy đơn hàng
            DatHang datHang = datHangService.findByMaDatHang(orderId);
            if (datHang == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
                return "redirect:/track-order";
            }

            // Kiểm tra quyền sở hữu đơn hàng
            if (!datHang.getNguoiDung().getMaNguoiDung().equals(nguoiDung.getMaNguoiDung())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đánh giá đơn hàng này");
                return "redirect:/track-order";
            }

            // Kiểm tra trạng thái đơn hàng
            if (!"Hoàn thành".equals(datHang.getTrangThai())) {
                redirectAttributes.addFlashAttribute("error", "Chỉ có thể đánh giá đơn hàng đã hoàn thành");
                return "redirect:/track-order";
            }

            // Kiểm tra đã đánh giá chưa
            if (danhGiaCuaHangService.existsByDatHang(datHang)) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã đánh giá đơn hàng này rồi");
                return "redirect:/track-order";
            }

            // Lấy thông tin sản phẩm trong đơn hàng
            List<DatHangChiTiet> chiTiets = datHang.getDatHangChiTiets();
            
            // Lấy thông tin cửa hàng (từ sản phẩm đầu tiên)
            CuaHang cuaHang = null;
            if (!chiTiets.isEmpty()) {
                cuaHang = chiTiets.get(0).getSanPham().getCuaHang();
            }

            // Thêm danh mục vào model (cho header)
            List<DanhMuc> danhMucs = danhMucService.findAllActiveCategories();
            model.addAttribute("danhMucs", danhMucs);
            model.addAttribute("nguoiDung", nguoiDung);
            model.addAttribute("datHang", datHang);
            model.addAttribute("cuaHang", cuaHang);
            model.addAttribute("chiTiets", chiTiets);

            return "web/review-form";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/track-order";
        }
    }
    
    @PostMapping("/submit")
    public String submitReview(
            @RequestParam Integer maDatHang,
            @RequestParam Integer maCuaHang,
            @RequestParam Integer soSaoCuaHang,
            @RequestParam String binhLuanCuaHang,
            @RequestParam Map<String, String> allParams,
            @RequestParam(required = false) Map<String, MultipartFile> files,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Kiểm tra đăng nhập
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return "redirect:/login";
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String email = userDetails.getUsername();

            // Tìm người dùng
            NguoiDung nguoiDung = nguoiDungService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Lấy đơn hàng
            DatHang datHang = datHangService.findByMaDatHang(maDatHang);
            if (datHang == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
                return "redirect:/track-order";
            }

            // Kiểm tra quyền sở hữu
            if (!datHang.getNguoiDung().getMaNguoiDung().equals(nguoiDung.getMaNguoiDung())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đánh giá đơn hàng này");
                return "redirect:/track-order";
            }

            // Kiểm tra đã đánh giá chưa
            if (danhGiaCuaHangService.existsByDatHang(datHang)) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã đánh giá đơn hàng này rồi");
                return "redirect:/track-order";
            }

            // Lưu đánh giá cửa hàng
            CuaHang cuaHang = cuaHangService.findByMaCuaHang(maCuaHang);
            if (cuaHang == null) {
                throw new RuntimeException("Không tìm thấy cửa hàng");
            }

            DanhGiaCuaHang danhGiaCuaHang = DanhGiaCuaHang.builder()
                    .cuaHang(cuaHang)
                    .nguoiDung(nguoiDung)
                    .datHang(datHang)
                    .soSao(soSaoCuaHang)
                    .binhLuan(binhLuanCuaHang)
                    .ngayDanhGia(new Date())
                    .build();

            danhGiaCuaHangService.save(danhGiaCuaHang);

            // Lưu đánh giá sản phẩm
            List<DatHangChiTiet> chiTiets = datHang.getDatHangChiTiets();
            for (DatHangChiTiet chiTiet : chiTiets) {
                Integer maSanPham = chiTiet.getSanPham().getMaSanPham();
                
                // Lấy số sao và bình luận từ params
                String soSaoKey = "soSao_" + maSanPham;
                String binhLuanKey = "binhLuan_" + maSanPham;
                
                if (allParams.containsKey(soSaoKey) && allParams.containsKey(binhLuanKey)) {
                    Integer soSao = Integer.parseInt(allParams.get(soSaoKey));
                    String binhLuan = allParams.get(binhLuanKey);
                    
                    // Xử lý upload file cho sản phẩm này
                    String fileName = null;
                    String fileKey = "anhVideo_" + maSanPham;
                    
                    if (files != null && files.containsKey(fileKey)) {
                        MultipartFile file = files.get(fileKey);
                        if (file != null && !file.isEmpty()) {
                            fileName = saveFile(file);
                        }
                    }
                    
                    // Lưu đánh giá sản phẩm
                    SanPham sanPham = sanPhamService.findByMaSanPham(maSanPham);
                    if (sanPham == null) {
                        throw new RuntimeException("Không tìm thấy sản phẩm");
                    }
                    
                    DanhGia danhGia = DanhGia.builder()
                            .sanPham(sanPham)
                            .nguoiDung(nguoiDung)
                            .soSao(soSao)
                            .binhLuan(binhLuan)
                            .anhVideo(fileName)
                            .ngayDanhGia(new Date())
                            .build();
                    
                    danhGiaRepository.save(danhGia);
                }
            }

            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");
            return "redirect:/track-order";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi lưu đánh giá: " + e.getMessage());
            return "redirect:/track-order";
        }
    }
    
    private String saveFile(MultipartFile file) throws IOException {
        // Tạo thư mục nếu chưa tồn tại
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // Tạo tên file unique
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        
        // Lưu file
        Files.write(filePath, file.getBytes());
        
        return fileName;
    }
}