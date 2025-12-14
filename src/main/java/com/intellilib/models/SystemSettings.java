package com.intellilib.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, name = "setting_key")
    private String key;
    
    @Column(nullable = false, name = "setting_value")
    private String value;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "setting_type")
    private String type = "TEXT"; // TEXT, NUMBER, BOOLEAN, DATE
    
    @Column(name = "category")
    private String category = "GENERAL";
    
    @Column(name = "is_editable")
    private Boolean isEditable = true;
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "last_modified")
    private java.time.LocalDateTime lastModified = java.time.LocalDateTime.now();
    
    public SystemSettings(String key, String value) {
        this.key = key;
        this.value = value;
    }
    
    public SystemSettings(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }
    
    // Helper methods to get typed values
    public Integer getIntValue() {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public Double getDoubleValue() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    public Boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }
}