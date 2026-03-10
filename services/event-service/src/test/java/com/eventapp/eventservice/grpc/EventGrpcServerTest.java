package com.eventapp.eventservice.grpc;

import com.eventapp.eventservice.exceptions.domain.EventNotFoundException;
import com.eventapp.eventservice.service.IEventService;
import com.eventapp.lib.event.v1.GetEventPriceRequest;
import com.eventapp.lib.event.v1.GetEventPriceResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event gRPC Server Functional Tests")
class EventGrpcServerTest {

    @Mock private IEventService eventService;
    @Mock private StreamObserver<GetEventPriceResponse> responseObserver;

    @InjectMocks private EventGrpcServer eventGrpcServer;

    private static final long TEST_EVENT_ID = 500L;
    private static final BigDecimal TEST_PRICE = new BigDecimal("125.50");

    @Nested
    @DisplayName("Price Retrieval via gRPC")
    class PriceRetrievalTests {

        @Test
        @DisplayName("Should map BigDecimal to String and complete the stream when price is found")
        void shouldReturnMappedPriceAndCompleteStream() {
            GetEventPriceRequest request = GetEventPriceRequest.newBuilder()
                    .setEventId(TEST_EVENT_ID)
                    .build();

            when(eventService.getPriceById(TEST_EVENT_ID))
                  .thenReturn(TEST_PRICE);

            eventGrpcServer.getEventPrice(request, responseObserver);

            ArgumentCaptor<GetEventPriceResponse> responseCaptor =
                    ArgumentCaptor.forClass(GetEventPriceResponse.class);
            
            verify(responseObserver, times(1)).onNext(responseCaptor.capture());
            verify(responseObserver, times(1)).onCompleted();
            verify(responseObserver, never()).onError(any());

            GetEventPriceResponse response = responseCaptor.getValue();
            assertThat(response.getEventId()).isEqualTo(TEST_EVENT_ID);
            assertThat(response.getPrice()).isEqualTo("125.50");
        }

        @Test
        @DisplayName("Should propagate EventNotFoundException to the global interceptor")
        void shouldPropagateException_WhenServiceThrowsNotFound() {
            GetEventPriceRequest request = GetEventPriceRequest.newBuilder()
                  .setEventId(TEST_EVENT_ID)
                  .build();

            // Simulate the service throwing the domain exception
            when(eventService.getPriceById(TEST_EVENT_ID))
                  .thenThrow(new EventNotFoundException(TEST_EVENT_ID));

            assertThatThrownBy(() -> eventGrpcServer.getEventPrice(request, responseObserver))
                  .isInstanceOf(EventNotFoundException.class);

            // Verify the stream was NOT closed or used by the server
            verify(responseObserver, never()).onNext(any());
            verify(responseObserver, never()).onCompleted();
            verify(responseObserver, never()).onError(any());
        }
    }
}