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

    // Tải hoặc truy xuất dữ liệu cho get data.
    public DashboardResponse getData(String from, String to, String trangThai) {
        String status = trangThai == null || trangThai.isBlank() ? null : trangThai.trim();
        DashboardResponse res = new DashboardResponse();
        double totalRevenue = nvl(repo.totalRevenue(from, to, status));
        double cashRevenue = nvl(repo.paymentRevenue(from, to, "CASH", status));

        res.setTotalOrder(repo.totalOrder(from, to, status));
        res.setTotalRevenue(totalRevenue);
        res.setCashRevenue(cashRevenue);
        res.setTransferRevenue(Math.max(0, totalRevenue - cashRevenue));
        res.setStatus(repo.status(from, to, status));
        res.setChannel(repo.channel(from, to, status));
        res.setChart(repo.chartData(from, to, status));
        res.setLowStock(repo.lowStock());
        res.setTopProduct(repo.topProduct(from, to, status));
        res.setTopCustomer(repo.topCustomer(from, to, status));
        return res;
    }

    // Tải hoặc truy xuất dữ liệu cho get top product by type.
    public List<Object[]> getTopProductByType(String type) {
        if ("today".equals(type)) {
            return repo.topProductToday();
        } else if ("week".equals(type)) {
            return repo.topProductWeek();
        } else {
            return repo.topProduct();
        }
    }

    // Thực hiện xử lý nghiệp vụ của hàm nvl.
    private double nvl(Double d) { return d == null ? 0 : d; }
}
