package com.example.sp.service;

import com.example.sp.dto.DashboardResponse;
import com.example.sp.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final DashboardRepository repo;

    public DashboardResponse getData(String from, String to) {
        DashboardResponse res = new DashboardResponse();
        res.setTotalOrder(repo.totalOrder(from, to));
        res.setTotalRevenue(nvl(repo.totalRevenue(from, to)));
        res.setRealRevenue(nvl(repo.realRevenue(from, to)));
        res.setStatus(repo.status());
        res.setChannel(repo.channel());
        res.setChart(repo.chartData());
        res.setLowStock(repo.lowStock());
        res.setTopProduct(repo.topProduct());
        return res;
    }

    public List<Object[]> getTopProductByType(String type) {
        if ("today".equals(type)) {
            return repo.topProductToday();
        } else if ("week".equals(type)) {
            return repo.topProductWeek();
        } else {
            return repo.topProduct();
        }
    }

    private double nvl(Double d) { return d == null ? 0 : d; }
}
