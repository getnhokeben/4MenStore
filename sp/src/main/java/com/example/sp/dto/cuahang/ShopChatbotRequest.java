package com.example.sp.dto.cuahang;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopChatbotRequest {

    @NotBlank(message = "Vui lòng nhập câu hỏi")
    @Size(max = 800, message = "Câu hỏi tối đa 800 ký tự")
    private String message;

    @Valid
    @Size(max = 10, message = "Chỉ gửi tối đa 10 tin nhắn gần nhất")
    private List<ShopChatbotMessageDTO> history = new ArrayList<>();
}
