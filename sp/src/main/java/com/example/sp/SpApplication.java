package com.example.sp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)   // ← thêm dòng này
public class SpApplication {
    // Thực hiện xử lý nghiệp vụ của hàm main.
    public static void main(String[] args) {
        SpringApplication.run(SpApplication.class, args);
    }
}
