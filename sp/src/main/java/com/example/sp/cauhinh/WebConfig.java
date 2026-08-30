package com.example.sp.cauhinh;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UploadStorageProperties uploadStorageProperties;

    @Override
    // Tạo hoặc cập nhật dữ liệu/trạng thái cho add resource handlers.
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadStorageProperties.resourceLocations());
    }
}
