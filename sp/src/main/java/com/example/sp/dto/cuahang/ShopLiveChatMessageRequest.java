package com.example.sp.dto.cuahang;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShopLiveChatMessageRequest {
    @NotBlank(message = "Vui lòng nhập tin nhắn")
    @Size(max = 1200, message = "Tin nhắn tối đa 1200 ký tự")
    private String message;
}
