package com.example.sp.dto.cuahang;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ShopChatbotResponse {
    private String reply;
    private List<String> suggestedSizes;
    private List<Integer> productIds;
    private ShopChatbotCriteriaDTO criteria;
    private List<ShopChatbotRecommendationDTO> recommendations;
    private List<String> quickReplies;
    private boolean needsClarification;
    private boolean aiPowered;
}
