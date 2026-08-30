package com.example.sp.repository.thongke;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class DashboardRepositoryQueryTest {

    @Autowired
    private DashboardRepository repository;

    @Test
    void dashboardQueriesSupportAllStatusesAndOptionalStatusFilter() {
        assertDoesNotThrow(() -> {
            repository.totalOrder(null, null, null);
            repository.totalRevenue(null, null, null);
            repository.paymentRevenue(null, null, "CASH", null);
            repository.chartData(null, null, null);
            repository.status(null, null, null);
            repository.channel(null, null, null);
            repository.topProduct(null, null, null);
            repository.topCustomer(null, null, null);

            repository.totalOrder(null, null, "Chờ thanh toán online");
            repository.status(null, null, "Chờ thanh toán online");
        });
    }
}
