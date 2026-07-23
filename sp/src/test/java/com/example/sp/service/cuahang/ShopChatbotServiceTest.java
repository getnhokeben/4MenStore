package com.example.sp.service.cuahang;

import com.example.sp.config.ShopChatbotProperties;
import com.example.sp.dto.cuahang.ShopChatbotRequest;
import com.example.sp.dto.cuahang.ShopChatbotResponse;
import com.example.sp.dto.cuahang.ShopChatbotMessageDTO;
import com.example.sp.dto.cuahang.ShopLookupDTO;
import com.example.sp.dto.cuahang.ShopProductDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShopChatbotServiceTest {

    private final ShopService shopService = mock(ShopService.class);
    private final ShopChatbotService service = new ShopChatbotService(
            shopService,
            new ObjectMapper(),
            new ShopChatbotProperties(),
            new ShopChatbotAdvisor()
    );

    @BeforeEach
    void defaultToEmptyCatalog() {
        when(shopService.getProducts(
                null, null, null, null, null, null, null, null,
                null, null, "newest", 0, 60
        )).thenReturn(Page.empty());
    }

    @Test
    void recommendsSizeFromVietnameseMetersAndKg() {
        assertEquals(List.of("L", "XL"), service.detectRecommendedSizes("Mình cao 1m72 và nặng 68kg"));
    }

    @Test
    void recommendsSizeFromCentimetersAcrossConversation() {
        assertEquals(List.of("XXL"), service.detectRecommendedSizes(
                "Khách: Mình cao 178 cm\nTrợ lý: Bạn nặng bao nhiêu?\nKhách: 82 kg"
        ));
    }

    @Test
    void waitsUntilBothMeasurementsAreAvailable() {
        assertEquals(List.of(), service.detectRecommendedSizes("Mình nặng 65 kg"));
    }

    @Test
    void repliesWithoutApiKeyUsingLocalSizeAdvisor() {
        ShopChatbotResponse response = service.reply(new ShopChatbotRequest(
                "Mình cao 1m72, nặng 68 kg thì mặc size gì?",
                List.of()
        ));

        assertFalse(response.isAiPowered());
        assertEquals(List.of("L", "XL"), response.getSuggestedSizes());
        assertTrue(response.getReply().contains("L–XL"));
        assertTrue(response.getCriteria().getSummary().contains("Size L–XL"));
    }

    @Test
    void returnsMatchingInStockProductIds() {
        ShopProductDTO polo = ShopProductDTO.builder()
                .idSp(7)
                .tenSp("Áo polo nam đen")
                .giaBanMin(new BigDecimal("350000"))
                .giaBanMax(new BigDecimal("350000"))
                .tongTon(12)
                .loaiAo("Áo polo")
                .kichCos(List.of(ShopLookupDTO.builder().id(1).ten("L").build()))
                .mauSacs(List.of(ShopLookupDTO.builder().id(2).ten("Đen").build()))
                .build();
        when(shopService.getProducts(
                null, null, null, null, null, null, null, null,
                null, null, "newest", 0, 60
        )).thenReturn(new PageImpl<>(List.of(polo)));

        ShopChatbotResponse response = service.reply(new ShopChatbotRequest(
                "Gợi ý áo polo màu đen dưới 400k",
                List.of()
        ));

        assertEquals(List.of(7), response.getProductIds());
        assertTrue(response.getReply().contains("Áo polo nam đen"));
        assertTrue(response.getRecommendations().get(0).getReasons().contains("Trong ngân sách"));
    }

    @Test
    void understandsVietnameseThousandBudgetWithoutRepeatingBestProduct() {
        ShopProductDTO affordable = product(15, "Áo polo đen vừa giá", "Đen", "250000");
        ShopProductDTO overBudget = product(16, "Áo polo đen cao cấp", "Đen", "320000");
        when(shopService.getProducts(
                null, null, null, null, null, null, null, null,
                null, null, "newest", 0, 60
        )).thenReturn(new PageImpl<>(List.of(overBudget, affordable)));

        ShopChatbotResponse response = service.reply(new ShopChatbotRequest(
                "Tìm áo polo màu đen dưới 300 nghìn",
                List.of()
        ));

        assertEquals(List.of(15), response.getProductIds());
        assertEquals(new BigDecimal("300000"), response.getCriteria().getMaxPrice());
        assertFalse(response.getReply().contains("ngoài ra có"));
    }

    @Test
    void remembersCriteriaFromPreviousUserMessages() {
        ShopProductDTO affordable = product(8, "Áo polo xanh", "Xanh", "320000");
        ShopProductDTO premium = product(9, "Áo polo xanh cao cấp", "Xanh", "480000");
        when(shopService.getProducts(
                null, null, null, null, null, null, null, null,
                null, null, "newest", 0, 60
        )).thenReturn(new PageImpl<>(List.of(premium, affordable)));

        ShopChatbotResponse response = service.reply(new ShopChatbotRequest(
                "Mẫu nào rẻ hơn?",
                List.of(new ShopChatbotMessageDTO("user", "Mình muốn áo polo xanh dưới 500k"))
        ));

        assertEquals(8, response.getProductIds().get(0));
        assertEquals("price_asc", response.getCriteria().getSortPreference());
        assertEquals(List.of("Xanh"), response.getCriteria().getPreferredColors());
    }

    @Test
    void respectsNegativeColorPreference() {
        ShopProductDTO black = product(10, "Áo polo đen", "Đen", "300000");
        ShopProductDTO blue = product(11, "Áo polo xanh", "Xanh", "330000");
        when(shopService.getProducts(
                null, null, null, null, null, null, null, null,
                null, null, "newest", 0, 60
        )).thenReturn(new PageImpl<>(List.of(black, blue)));

        ShopChatbotResponse response = service.reply(new ShopChatbotRequest(
                "Gợi ý áo polo nhưng không lấy màu đen",
                List.of()
        ));

        assertEquals(List.of(11), response.getProductIds());
        assertEquals(List.of("Đen"), response.getCriteria().getExcludedColors());
    }

    @Test
    void latestColorRequestOverridesRememberedColor() {
        ShopProductDTO black = product(12, "Áo polo đen", "Đen", "300000");
        ShopProductDTO blue = product(13, "Áo polo xanh", "Xanh", "330000");
        when(shopService.getProducts(
                null, null, null, null, null, null, null, null,
                null, null, "newest", 0, 60
        )).thenReturn(new PageImpl<>(List.of(black, blue)));

        ShopChatbotResponse response = service.reply(new ShopChatbotRequest(
                "Đổi sang màu xanh",
                List.of(new ShopChatbotMessageDTO("user", "Mình muốn áo polo đen"))
        ));

        assertEquals(List.of(13), response.getProductIds());
        assertEquals(List.of("Xanh"), response.getCriteria().getPreferredColors());
    }

    @Test
    void canClearRememberedPriceLimit() {
        ShopProductDTO premium = product(14, "Áo polo cao cấp", "Xanh", "650000");
        when(shopService.getProducts(
                null, null, null, null, null, null, null, null,
                null, null, "newest", 0, 60
        )).thenReturn(new PageImpl<>(List.of(premium)));

        ShopChatbotResponse response = service.reply(new ShopChatbotRequest(
                "Bỏ giới hạn giá",
                List.of(new ShopChatbotMessageDTO("user", "Tìm áo polo dưới 400k"))
        ));

        assertEquals(List.of(14), response.getProductIds());
        assertEquals(null, response.getCriteria().getMaxPrice());
    }

    private ShopProductDTO product(int id, String name, String color, String price) {
        return ShopProductDTO.builder()
                .idSp(id)
                .tenSp(name)
                .giaBanMin(new BigDecimal(price))
                .giaBanMax(new BigDecimal(price))
                .tongTon(10)
                .loaiAo("Áo polo")
                .kichCos(List.of(ShopLookupDTO.builder().id(1).ten("L").build()))
                .mauSacs(List.of(ShopLookupDTO.builder().id(2).ten(color).build()))
                .build();
    }
}
