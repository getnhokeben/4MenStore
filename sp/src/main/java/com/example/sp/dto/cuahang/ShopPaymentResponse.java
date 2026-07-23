package com.example.sp.dto.cuahang;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopPaymentResponse {
    private String gateway;
    private String paymentUrl;
    private String transactionRef;
}
