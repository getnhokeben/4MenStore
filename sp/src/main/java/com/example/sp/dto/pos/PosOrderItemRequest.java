package com.example.sp.dto.pos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PosOrderItemRequest {
    @NotNull(message = "Thiếu biến thể sản phẩm")
    private Integer idSpct;

    @NotNull(message = "Thiếu số lượng")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;
}
