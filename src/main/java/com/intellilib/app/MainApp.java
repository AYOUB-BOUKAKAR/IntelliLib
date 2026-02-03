package com.intellilib.app;

import com.intellilib.util.FXMLLoaderUtil;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.intellilib")
@EnableJpaRepositories(basePackages = "com.intellilib.repositories")
@EntityScan(basePackages = "com.intellilib.models")
public class MainApp extends Application {
    
    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        launch(MainApp.class, args);
    }

    @Override
    public void init() throws Exception {
        // Start Spring context
        springContext = SpringApplication.run(MainApp.class);
        
        // Set Spring context for FXMLLoaderUtil
        FXMLLoaderUtil.setApplicationContext(springContext);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("IntelliLib - Welcome");
        primaryStage.setScene(FXMLLoaderUtil.loadScene("/views/main.fxml"));
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }
    
    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}