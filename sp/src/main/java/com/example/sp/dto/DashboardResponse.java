package com.example.sp.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardResponse {
    private int totalOrder;
    private double totalRevenue;
    private double realRevenue;
    private List<Object[]> status;
    private List<Object[]> channel;
    private List<Object[]> chart;
    private List<Object[]> lowStock;
    private List<Object[]> topProduct;
}
