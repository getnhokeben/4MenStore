package com.example.sp.dto.cuahang;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopChatbotMessageDTO {

    @Pattern(regexp = "user|assistant", message = "Vai trò hội thoại không hợp lệ")
    private String role;

    @Size(max = 800, message = "Nội dung hội thoại tối đa 800 ký tự")
    private String content;
}
