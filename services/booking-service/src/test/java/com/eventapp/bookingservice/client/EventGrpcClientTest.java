package com.eventapp.bookingservice.client;

import com.eventapp.lib.event.v1.EventServiceGrpc;
import com.eventapp.lib.event.v1.GetEventPriceRequest;
import com.eventapp.lib.event.v1.GetEventPriceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventGrpcClientTest {

    @Mock private EventServiceGrpc.EventServiceBlockingStub eventStub;
    @InjectMocks private EventGrpcClient eventGrpcClient;

    @Test
    @DisplayName("Should return price when gRPC service returns successful response")
    void shouldReturnPrice_WhenGrpcCallSucceeds() {
        GetEventPriceResponse mockResponse = GetEventPriceResponse.newBuilder()
              .setPrice("99.99")
              .build();

        when(eventStub.getEventPrice(any(GetEventPriceRequest.class)))
              .thenReturn(mockResponse);

        BigDecimal result = eventGrpcClient.fetchEventPrice(1L);

        assertThat(result)
              .isEqualByComparingTo("99.99");

        verify(eventStub).getEventPrice(argThat(request -> request.getEventId() == 1L));
    }
}