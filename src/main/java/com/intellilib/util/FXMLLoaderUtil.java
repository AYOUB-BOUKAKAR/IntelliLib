package com.intellilib.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FXMLLoaderUtil {
    
    private static ApplicationContext context;
    
    public static void setApplicationContext(ApplicationContext ctx) {
        context = ctx;
    }
    
    public static Parent load(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(FXMLLoaderUtil.class.getResource(fxmlPath));
        loader.setControllerFactory(context::getBean);
        return loader.load();
    }
    
    public static Stage loadStage(String fxmlPath, String title, boolean maximized) throws IOException {
        Parent root = load(fxmlPath);
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        if (maximized) {
            stage.setMaximized(true);
        }
        return stage;
    }
}