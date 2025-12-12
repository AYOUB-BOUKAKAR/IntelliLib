package com.intellilib.services;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DatabaseService {
    
    private static final String DB_BACKUP_DIR = "database_backups";
    
    public boolean backupDatabase() {
        try {
            // Create backup directory if it doesn't exist
            File backupDir = new File(DB_BACKUP_DIR);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // Generate timestamp for the backup file
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = String.format("intellilib_backup_%s.db", timestamp);
            
            // Get the database file path (adjust this based on your actual database configuration)
            String dbFilePath = "intellilib.db"; // Default SQLite database file
            
            // Copy the database file to the backup location
            Path source = Paths.get(dbFilePath);
            Path target = Paths.get(DB_BACKUP_DIR, backupFileName);
            
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
