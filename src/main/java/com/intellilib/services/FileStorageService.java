package com.intellilib.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileStorageService {
    
    private final Path rootLocation = Paths.get("uploads/books");
    
    public FileStorageService() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }
    
    public String storeFile(MultipartFile file, String isbn) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueFilename = isbn + "_" + timestamp + fileExtension;
        
        // Create folder structure: uploads/books/{first-3-of-isbn}/{rest-of-isbn}/
        String folderPath = isbn.substring(0, Math.min(3, isbn.length()));
        Path targetFolder = rootLocation.resolve(folderPath).resolve(isbn);
        Files.createDirectories(targetFolder);
        
        // Save file
        Path targetLocation = targetFolder.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path
        return folderPath + "/" + isbn + "/" + uniqueFilename;
    }
    
    public Path loadFile(String filePath) {
        return rootLocation.resolve(filePath).normalize().toAbsolutePath();
    }
    
    public void deleteFile(String filePath) throws IOException {
        if (filePath != null && !filePath.isEmpty()) {
            Path fileToDelete = rootLocation.resolve(filePath).normalize().toAbsolutePath();
            Files.deleteIfExists(fileToDelete);
            
            // Try to delete empty parent directories
            deleteEmptyParentDirectories(fileToDelete.getParent());
        }
    }
    
    private void deleteEmptyParentDirectories(Path directory) throws IOException {
        if (directory != null && Files.exists(directory)) {
            try (var stream = Files.list(directory)) {
                if (stream.findAny().isEmpty()) {
                    Files.delete(directory);
                    deleteEmptyParentDirectories(directory.getParent());
                }
            }
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) return ".pdf";
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? ".pdf" : filename.substring(lastDot).toLowerCase();
    }
    
    public String getMimeType(String filename) {
        String extension = getFileExtension(filename);
        return switch (extension.toLowerCase()) {
            case ".pdf" -> "application/pdf";
            case ".txt" -> "text/plain";
            case ".epub" -> "application/epub+zip";
            case ".mobi" -> "application/x-mobipocket-ebook";
            default -> "application/octet-stream";
        };
    }
}