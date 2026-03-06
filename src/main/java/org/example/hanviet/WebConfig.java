package org.example.hanviet;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps the physical "images" directory in your project root to the "/image/**" URL
        Path imageDir = Paths.get("uploads");
        String imagePath = imageDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/image/**")
                .addResourceLocations("file:/" + imagePath + "/");
    }
}