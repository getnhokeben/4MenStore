package com.example.sp.service.thongke;

import com.example.sp.repository.thongke.DashboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository repository;

    @InjectMocks
    private DashboardService service;

    @Test
    void forwardsTrimmedOrderStatusToEveryDashboardQuery() {
        String from = "2026-07-01";
        String to = "2026-07-31";
        String status = "Chờ thanh toán online";

        service.getData(from, to, "  " + status + "  ");

        verify(repository).totalOrder(from, to, status);
        verify(repository).totalRevenue(from, to, status);
        verify(repository).paymentRevenue(from, to, "CASH", status);
        verify(repository).status(from, to, status);
        verify(repository).channel(from, to, status);
        verify(repository).chartData(from, to, status);
        verify(repository).topProduct(from, to, status);
        verify(repository).topCustomer(from, to, status);
    }
}
