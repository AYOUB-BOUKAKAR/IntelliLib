package com.intellilib.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;

public class StandardMultipartFile implements MultipartFile {
    
    private final File file;
    private final String name;
    
    public StandardMultipartFile(File file, String name) {
        this.file = file;
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getOriginalFilename() {
        return file.getName();
    }
    
    @Override
    public String getContentType() {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".epub")) return "application/epub+zip";
        return "application/octet-stream";
    }
    
    @Override
    public boolean isEmpty() {
        return file.length() == 0;
    }
    
    @Override
    public long getSize() {
        return file.length();
    }
    
    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(file.toPath());
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }
    
    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}