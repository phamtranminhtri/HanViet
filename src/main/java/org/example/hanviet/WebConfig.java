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
        // Use toAbsolutePath() and toUri() to safely generate cross-platform file URIs
        Path imageDir = Paths.get("uploads").toAbsolutePath();

        registry.addResourceHandler("/image/**")
                .addResourceLocations(imageDir.toUri().toString() + "/");
    }
}