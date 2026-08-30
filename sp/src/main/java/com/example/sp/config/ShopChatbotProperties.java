package com.example.sp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "chatbot.openai")
public class ShopChatbotProperties {

    private boolean enabled = true;
    private String apiKey = "";
    private String model = "gpt-5.4-mini";
    private String endpoint = "https://api.openai.com/v1/responses";
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
    private int maxOutputTokens = 450;

    // Kiểm tra điều kiện và tính hợp lệ cho is configured.
    public boolean isConfigured() {
        return enabled
                && apiKey != null
                && !apiKey.isBlank()
                && model != null
                && !model.isBlank()
                && endpoint != null
                && !endpoint.isBlank();
    }
}
