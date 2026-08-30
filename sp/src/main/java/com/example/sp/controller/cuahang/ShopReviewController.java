package com.example.sp.controller.cuahang;

import com.example.sp.dto.cuahang.ShopReviewDTO;
import com.example.sp.dto.cuahang.ShopReviewRequest;
import com.example.sp.dto.cuahang.ShopReviewSummaryDTO;
import com.example.sp.service.cuahang.ShopReviewService;
import com.example.sp.service.cuahang.ShopSessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShopReviewController {

    private final ShopReviewService shopReviewService;

    @GetMapping("/api/shop/products/{productId}/reviews")
    // Thực hiện xử lý nghiệp vụ của hàm reviews.
    public ShopReviewSummaryDTO reviews(@PathVariable Integer productId) {
        return shopReviewService.getVisibleReviews(productId);
    }

    @GetMapping("/api/shop/reviews/mine")
    // Thực hiện xử lý nghiệp vụ của hàm my reviews.
    public List<ShopReviewDTO> myReviews(HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(ShopSessionKeys.CUSTOMER_ID);
        return shopReviewService.getMyReviews(customerId);
    }

    @PostMapping("/api/shop/products/{productId}/reviews")
    // Xử lý tương tác người dùng cho submit.
    public ResponseEntity<ShopReviewDTO> submit(@PathVariable Integer productId,
                                                 @Valid @RequestBody ShopReviewRequest request,
                                                 HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(ShopSessionKeys.CUSTOMER_ID);
        return ResponseEntity.ok(shopReviewService.submit(productId, customerId, request));
    }
}
