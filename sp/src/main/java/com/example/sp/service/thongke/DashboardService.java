package com.example.sp.service.thongke;

import com.example.sp.dto.thongke.DashboardResponse;
import com.example.sp.repository.thongke.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final DashboardRepository repo;

    public DashboardResponse getData(String from, String to) {
        DashboardResponse res = new DashboardResponse();
        double totalRevenue = nvl(repo.totalRevenue(from, to));
        double cashRevenue = nvl(repo.paymentRevenue(from, to, "CASH"));

        res.setTotalOrder(repo.totalOrder(from, to));
        res.setTotalRevenue(totalRevenue);
        res.setCashRevenue(cashRevenue);
        res.setTransferRevenue(Math.max(0, totalRevenue - cashRevenue));
        res.setStatus(repo.status(from, to));
        res.setChannel(repo.channel(from, to));
        res.setChart(repo.chartData(from, to));
        res.setLowStock(repo.lowStock());
        res.setTopProduct(repo.topProduct(from, to));
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
