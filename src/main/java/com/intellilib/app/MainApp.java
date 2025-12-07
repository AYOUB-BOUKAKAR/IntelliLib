package com.intellilib.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.intellilib")
@EnableJpaRepositories(basePackages = "com.intellilib.repositories")
@EntityScan(basePackages = "com.intellilib.models")
public class MainApp extends Application {
    
    private static ConfigurableApplicationContext springContext;
    private Parent root;

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }

    @Override
    public void init() throws Exception {
        springContext = SpringApplication.run(MainApp.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
        loader.setControllerFactory(springContext::getBean);
        
        root = loader.load();
        primaryStage.setTitle("IntelliLib - Login");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }
    
    public static Parent loadFXML(String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxmlPath));
        loader.setControllerFactory(springContext::getBean);
        return loader.load();
    }
    
    public static <T> T getController(Class<T> controllerClass) {
        return springContext.getBean(controllerClass);
    }
}