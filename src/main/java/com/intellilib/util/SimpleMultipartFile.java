package com.intellilib.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// Simple MultipartFile implementation
 public class SimpleMultipartFile implements MultipartFile {
    private final File file;
    private final String contentType;
    private final byte[] content;

    public SimpleMultipartFile(File file) throws IOException {
        this.file = file;
        String detectedType = Files.probeContentType(file.toPath());
        this.contentType = (detectedType != null) ? detectedType : "application/pdf";
        this.content = Files.readAllBytes(file.toPath());
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return file.getName();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public java.io.InputStream getInputStream() throws IOException {
        return new java.io.ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.copy(getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}