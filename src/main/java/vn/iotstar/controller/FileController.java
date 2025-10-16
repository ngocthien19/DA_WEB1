package vn.iotstar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.service.impl.FileStorageService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/download/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            HttpServletRequest request) {

        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            contentType = "application/octet-stream";
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/uploads/users/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveUserImage(
            @PathVariable String fileName,
            HttpServletRequest request) {

        try {
            // Tạo đường dẫn đến file trong folder uploads/users
            String projectPath = System.getProperty("user.dir");
            String userImagePath = projectPath + "/uploads/users/" + fileName;

            Resource resource = new UrlResource(Paths.get(userImagePath).toUri());

            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                contentType = "application/octet-stream";
            }

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception ex) {
            throw new RuntimeException("Could not load file: " + fileName, ex);
        }
    }

    // Thêm endpoint API upload ảnh cửa hàng
    @PostMapping("/api/upload/store-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadStoreImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== UPLOAD STORE IMAGE START ===");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());

            // Kiểm tra file có rỗng không
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra loại file (chỉ chấp nhận ảnh)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "Chỉ chấp nhận file ảnh (JPG, PNG, GIF)");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra kích thước file (tối đa 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "Kích thước file không được vượt quá 5MB");
                return ResponseEntity.badRequest().body(response);
            }

            // Tạo thư mục stores nếu chưa tồn tại
            String projectPath = System.getProperty("user.dir");
            String storeImageDir = projectPath + "/uploads/stores";
            Path storeImagePath = Paths.get(storeImageDir);

            if (!Files.exists(storeImagePath)) {
                Files.createDirectories(storeImagePath);
                System.out.println("Created stores directory: " + storeImagePath);
            }

            // Tạo tên file unique
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + fileExtension;

            // Lưu file vào thư mục uploads/stores
            Path targetLocation = storeImagePath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Trả về đường dẫn file để lưu vào database
            String filePath = "/uploads/stores/" + newFilename;

            System.out.println("File saved successfully: " + targetLocation);
            System.out.println("File path for database: " + filePath);
            System.out.println("=== UPLOAD STORE IMAGE SUCCESS ===");

            response.put("success", true);
            response.put("message", "Upload ảnh thành công");
            response.put("filePath", filePath);
            response.put("fileName", newFilename);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("=== UPLOAD STORE IMAGE ERROR ===");
            System.err.println("Error uploading store image: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");

            response.put("success", false);
            response.put("message", "Lỗi khi upload ảnh: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Thêm endpoint API upload file chat
    @PostMapping("/api/upload/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadChatFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== UPLOAD CHAT FILE START ===");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());

            // Kiểm tra file có rỗng không
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra kích thước file (tối đa 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "Kích thước file không được vượt quá 10MB");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra loại file (ảnh, video, pdf)
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.startsWith("image/") &&
                            !contentType.startsWith("video/") &&
                            !contentType.equals("application/pdf"))) {
                response.put("success", false);
                response.put("message", "Chỉ chấp nhận file ảnh, video hoặc PDF");
                return ResponseEntity.badRequest().body(response);
            }

            // Tạo thư mục chats nếu chưa tồn tại
            String projectPath = System.getProperty("user.dir");
            String chatFileDir = projectPath + "/uploads/chats";
            Path chatFilePath = Paths.get(chatFileDir);

            if (!Files.exists(chatFilePath)) {
                Files.createDirectories(chatFilePath);
                System.out.println("Created chats directory: " + chatFilePath);
            }

            // Tạo tên file unique
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + fileExtension;

            // Lưu file vào thư mục uploads/chats
            Path targetLocation = chatFilePath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Trả về đường dẫn file để lưu vào database
            String filePath = "/uploads/chats/" + newFilename;

            System.out.println("File saved successfully: " + targetLocation);
            System.out.println("File path for database: " + filePath);
            System.out.println("=== UPLOAD CHAT FILE SUCCESS ===");

            response.put("success", true);
            response.put("message", "Upload file thành công");
            response.put("filePath", filePath);
            response.put("fileName", newFilename);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("=== UPLOAD CHAT FILE ERROR ===");
            System.err.println("Error uploading chat file: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");

            response.put("success", false);
            response.put("message", "Lỗi khi upload file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Serve chat files
    @GetMapping("/uploads/chats/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveChatFile(
            @PathVariable String fileName,
            HttpServletRequest request) {

        try {
            // Tạo đường dẫn đến file trong folder uploads/chats
            String projectPath = System.getProperty("user.dir");
            String chatFilePath = projectPath + "/uploads/chats/" + fileName;

            Resource resource = new UrlResource(Paths.get(chatFilePath).toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found: " + fileName);
            }

            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                contentType = "application/octet-stream";
            }

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception ex) {
            throw new RuntimeException("Could not load chat file: " + fileName, ex);
        }
    }

    @GetMapping("/uploads/stores/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveStoreImage(
            @PathVariable String fileName,
            HttpServletRequest request) {

        try {
            // Tạo đường dẫn đến file trong folder uploads/stores
            String projectPath = System.getProperty("user.dir");
            String storeImagePath = projectPath + "/uploads/stores/" + fileName;

            Resource resource = new UrlResource(Paths.get(storeImagePath).toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found: " + fileName);
            }

            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                contentType = "application/octet-stream";
            }

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception ex) {
            throw new RuntimeException("Could not load store image: " + fileName, ex);
        }
    }
}