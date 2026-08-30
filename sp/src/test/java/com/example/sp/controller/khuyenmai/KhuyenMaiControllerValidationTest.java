package com.example.sp.controller.khuyenmai;

import com.example.sp.dto.khuyenmai.DotGiamGiaRequest;
import com.example.sp.dto.khuyenmai.PhieuGiamGiaRequest;
import com.example.sp.service.khuyenmai.DotGiamGiaService;
import com.example.sp.service.khuyenmai.PhieuGiamGiaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class KhuyenMaiControllerValidationTest {

    @Test
    void createVoucherClearsClientControlledId() {
        PhieuGiamGiaService service = mock(PhieuGiamGiaService.class);
        PhieuGiamGiaController controller = new PhieuGiamGiaController(service);
        PhieuGiamGiaRequest request = new PhieuGiamGiaRequest();
        request.setId(99);

        controller.create(request);

        ArgumentCaptor<PhieuGiamGiaRequest> captor = ArgumentCaptor.forClass(PhieuGiamGiaRequest.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    void createVoucherRejectsNullPayloadBeforeCallingService() {
        PhieuGiamGiaService service = mock(PhieuGiamGiaService.class);
        PhieuGiamGiaController controller = new PhieuGiamGiaController(service);

        assertThatThrownBy(() -> controller.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dữ liệu phiếu giảm giá không hợp lệ");

        verifyNoInteractions(service);
    }

    @Test
    void createPromotionClearsClientControlledId() {
        DotGiamGiaService service = mock(DotGiamGiaService.class);
        DotGiamGiaController controller = new DotGiamGiaController(service);
        DotGiamGiaRequest request = new DotGiamGiaRequest();
        request.setId(99);

        controller.create(request);

        ArgumentCaptor<DotGiamGiaRequest> captor = ArgumentCaptor.forClass(DotGiamGiaRequest.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    void createPromotionRejectsNullPayloadBeforeCallingService() {
        DotGiamGiaService service = mock(DotGiamGiaService.class);
        DotGiamGiaController controller = new DotGiamGiaController(service);

        assertThatThrownBy(() -> controller.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dữ liệu đợt giảm giá không hợp lệ");

        verifyNoInteractions(service);
    }
}
