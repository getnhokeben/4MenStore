package com.example.sp.dto.cuahang;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopReviewRequest {

    @NotNull(message = "Vui lòng chọn số sao đánh giá")
    @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5 sao")
    @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5 sao")
    private Integer diemDanhGia;

    @Size(max = 1000, message = "Nội dung đánh giá tối đa 1000 ký tự")
    private String noiDung;
}
