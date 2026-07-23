package com.example.sp.dto.cuahang;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopChatbotCriteriaDTO {
    private Integer heightCm;
    private Integer weightKg;
    private List<String> preferredSizes;
    private List<String> preferredColors;
    private List<String> excludedColors;
    private List<String> categories;
    private List<String> materials;
    private List<String> styles;
    private List<String> fits;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String fitPreference;
    private String occasion;
    private String sortPreference;
    private String summary;
}
