package vn.iotstar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get absolute path to project uploads directory
        String projectRoot = System.getProperty("user.dir");
        String uploadsPath = projectRoot + File.separator + "uploads" + File.separator;
        
        // Convert to file URL format
        String uploadsUrl = "file:" + uploadsPath;
        
        // Map /uploads/** URLs to the uploads directory in project root
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsUrl);
        
        // Static resources from classpath
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
                
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/");
    }
}