package com.example.sp.controller.cuahang;

import com.example.sp.dto.cuahang.ShopChatbotRequest;
import com.example.sp.dto.cuahang.ShopChatbotResponse;
import com.example.sp.service.cuahang.ShopChatbotService;
import com.example.sp.service.cuahang.ShopLiveChatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shop/chatbot")
public class ShopChatbotController {

    private final ShopChatbotService chatbotService;
    private final ShopLiveChatService liveChatService;

    @PostMapping
    public ResponseEntity<ShopChatbotResponse> chat(@Valid @RequestBody ShopChatbotRequest request,
                                                    HttpSession session) {
        ShopChatbotResponse response = chatbotService.reply(request);
        liveChatService.recordAiExchange(session, request.getMessage(), response.getReply());
        return ResponseEntity.ok(response);
    }
}
