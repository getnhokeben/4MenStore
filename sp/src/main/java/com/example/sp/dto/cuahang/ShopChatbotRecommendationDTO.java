package com.example.sp.dto.cuahang;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopChatbotRecommendationDTO {
    private Integer productId;
    private int matchScore;
    private List<String> reasons;
    private String preferredSize;
    private String preferredColor;
}
