package com.project.pas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.uploadDir, e);
        }
    }

    /**
     * Stores a file and returns the relative path (from upload root).
     */
    public String storeFile(MultipartFile file, String subdirectory) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String storedFilename = UUID.randomUUID().toString() + extension;

            Path targetDir = uploadDir.resolve(subdirectory).normalize();
            if (!targetDir.startsWith(uploadDir)) {
                throw new SecurityException("Cannot store file outside current directory");
            }
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedFilename).normalize();
            if (!targetPath.startsWith(uploadDir)) {
                throw new SecurityException("Cannot store file outside current directory");
            }
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return subdirectory + "/" + storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * Returns the absolute path for a stored file.
     */
    public Path getFilePath(String relativePath) {
        Path resolvedPath = uploadDir.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(uploadDir)) {
            throw new SecurityException("Cannot read file outside current directory");
        }
        return resolvedPath;
    }

    /**
     * Reads all lines from a CSV file.
     */
    public java.util.List<String> readCsvLines(MultipartFile file) {
        try {
            String content = new String(file.getBytes());
            return java.util.Arrays.asList(content.split("\\r?\\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file", e);
        }
    }
}
