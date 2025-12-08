package com.intellilib.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
public class FXMLLoaderUtil {
    
    private static ApplicationContext applicationContext;
    
    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }
    
    public static Scene loadScene(String fxmlPath) throws IOException {
        URL fxmlUrl = FXMLLoaderUtil.class.getResource(fxmlPath);
        
        if (fxmlUrl == null) {
            throw new IOException("FXML file not found: " + fxmlPath);
        }
        
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        
        // Use Spring to create controllers
        if (applicationContext != null) {
            loader.setControllerFactory(applicationContext::getBean);
        }
        
        Parent root = loader.load();
        return new Scene(root);
    }
    
    public static Stage loadStage(String fxmlPath, String title, boolean maximized) throws IOException {
        Stage stage = new Stage();
        Scene scene = loadScene(fxmlPath);
        stage.setScene(scene);
        stage.setTitle(title);
        
        if (maximized) {
            stage.setMaximized(true);
        }
        
        return stage;
    }
    
    public static <T> T loadController(String fxmlPath) throws IOException {
        URL fxmlUrl = FXMLLoaderUtil.class.getResource(fxmlPath);
        
        if (fxmlUrl == null) {
            throw new IOException("FXML file not found: " + fxmlPath);
        }
        
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        
        if (applicationContext != null) {
            loader.setControllerFactory(applicationContext::getBean);
        }
        
        loader.load();
        return loader.getController();
    }
}