package za.co.tuks.amrdashboard.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDirectory;

    public FileStorageService() {
        this.uploadDirectory = Paths.get("uploads/blog-images").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new RuntimeException("Invalid file path: " + originalFileName);
        }

        String extension = StringUtils.getFilenameExtension(originalFileName);
        String storedFileName = UUID.randomUUID().toString();
        if (extension != null && !extension.isBlank()) {
            storedFileName += "." + extension;
        }

        Path targetLocation = this.uploadDirectory.resolve(storedFileName);
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/blog-images/" + storedFileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + storedFileName, e);
        }
    }
}
