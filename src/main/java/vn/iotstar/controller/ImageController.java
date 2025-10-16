package vn.iotstar.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ImageController {

    @GetMapping("/uploads/users/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveUserImage(
            @PathVariable String fileName,
            HttpServletRequest request) {

        System.out.println("=== SERVE USER IMAGE START ===");
        System.out.println("Requested file: " + fileName);

        try {
            // Tạo đường dẫn đến file trong folder uploads/users
            String projectPath = System.getProperty("user.dir");
            String userImagePath = projectPath + File.separator + "uploads" + File.separator + "users" + File.separator + fileName;
            
            System.out.println("Project path: " + projectPath);
            System.out.println("Full image path: " + userImagePath);
            
            Path filePath = Paths.get(userImagePath);
            Resource resource = new UrlResource(filePath.toUri());

            // Check if file exists and log details
            File imageFile = new File(userImagePath);
            System.out.println("File exists: " + imageFile.exists());
            if (imageFile.exists()) {
                System.out.println("File size: " + imageFile.length() + " bytes");
                System.out.println("File is readable: " + imageFile.canRead());
            } else {
                System.out.println("File does not exist at path: " + userImagePath);
                
                // List all files in uploads/users directory for debugging
                File uploadDir = new File(projectPath + File.separator + "uploads" + File.separator + "users");
                if (uploadDir.exists() && uploadDir.isDirectory()) {
                    System.out.println("Files in uploads/users directory:");
                    File[] files = uploadDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            System.out.println("  - " + f.getName() + " (size: " + f.length() + " bytes)");
                        }
                    } else {
                        System.out.println("  No files found or cannot read directory");
                    }
                } else {
                    System.out.println("uploads/users directory does not exist or is not a directory");
                }
            }

            if (!resource.exists() || !resource.isReadable()) {
                System.out.println("User image not found or not readable: " + userImagePath);
                System.out.println("=== SERVE USER IMAGE FAILED ===");
                
                // Return default avatar instead of 404
                return serveDefaultAvatar();
            }

            // Tự động detect content type từ file extension
            String contentType = "image/jpeg"; // default
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (lowerFileName.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (lowerFileName.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }

            System.out.println("Serving user image successfully: " + fileName);
            System.out.println("Content type: " + contentType);
            System.out.println("=== SERVE USER IMAGE SUCCESS ===");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache 1 giờ
                    .body(resource);

        } catch (Exception ex) {
            System.err.println("=== SERVE USER IMAGE ERROR ===");
            System.err.println("Error serving user image: " + fileName + " - " + ex.getMessage());
            ex.printStackTrace();
            System.err.println("=== END ERROR ===");
            
            // Return default avatar instead of 404
            return serveDefaultAvatar();
        }
    }

    /**
     * Serve default avatar when user image is not found
     */
    private ResponseEntity<Resource> serveDefaultAvatar() {
        try {
            // Try to serve a default avatar from static resources
            String projectPath = System.getProperty("user.dir");
            String defaultAvatarPath = projectPath + File.separator + "src" + File.separator + "main" + 
                                     File.separator + "resources" + File.separator + "static" + 
                                     File.separator + "images" + File.separator + "default-avatar.jpg";
            
            Path defaultPath = Paths.get(defaultAvatarPath);
            Resource defaultResource = new UrlResource(defaultPath.toUri());
            
            if (defaultResource.exists() && defaultResource.isReadable()) {
                System.out.println("Serving default avatar from: " + defaultAvatarPath);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                        .body(defaultResource);
            } else {
                System.out.println("Default avatar not found, returning 404");
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            System.err.println("Error serving default avatar: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/uploads/images/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveGeneralImage(
            @PathVariable String fileName,
            HttpServletRequest request) {

        try {
            // Tạo đường dẫn đến file trong folder uploads/images
            String projectPath = System.getProperty("user.dir");
            String imagePath = projectPath + File.separator + "uploads" + File.separator + "images" + File.separator + fileName;
            
            Path filePath = Paths.get(imagePath);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                System.out.println("General image not found: " + imagePath); 
                return ResponseEntity.notFound().build();
            }

            // Tự động detect content type từ file extension
            String contentType = "image/jpeg"; // default
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (lowerFileName.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (lowerFileName.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }

            System.out.println("Serving general image: " + fileName + " from: " + imagePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(resource);

        } catch (Exception ex) {
            System.err.println("Error serving general image: " + fileName + " - " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
    
    
}
