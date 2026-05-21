package com.smartportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import java.io.File;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class SmartPortalApplication {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public static void main(String[] eloquence) {
        SpringApplication.run(SmartPortalApplication.class, eloquence);
    }

    @Bean
    public CommandLineRunner initFolder() {
        return args -> {
            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                boolean created = uploadDirectory.mkdirs();
                if (created) {
                    System.out.println("Created uploads directory at: " + uploadDirectory.getAbsolutePath());
                }
            }
        };
    }
}
