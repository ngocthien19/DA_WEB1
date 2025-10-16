package vn.iotstar.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${upload.path:uploads/images/}") String uploadPath) {
        // Use absolute path from project root instead of relative path
        String projectPath = System.getProperty("user.dir");
        String absoluteUploadPath = projectPath + File.separator + uploadPath.replace("/", File.separator);
        
        this.fileStorageLocation = Paths.get(absoluteUploadPath).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            System.out.println("Upload directory created: " + this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory!", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }

            // Get original filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Generate unique filename
            String newFilename = UUID.randomUUID().toString() + fileExtension;

            // Copy file to target location
            Path targetLocation = this.fileStorageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return newFilename; // CHỈ TRẢ VỀ TÊN FILE, KHÔNG CÓ "/uploads/"
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + fileName, ex);
        }
    }

    public String getFileUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "/images/default-product.jpg";
        }
        
        // Nếu file name đã là full URL (từ database cũ)
        if (fileName.startsWith("http") || fileName.startsWith("/uploads/")) {
            return fileName;
        }
        
        // QUAN TRỌNG: Trả về đường dẫn bắt đầu bằng /uploads/
        return "/uploads/" + fileName;
    }
}