package com.eventapp.bookingservice.client;


import com.eventapp.lib.event.v1.EventServiceGrpc;
import com.eventapp.lib.event.v1.GetEventPriceRequest;
import com.eventapp.lib.event.v1.GetEventPriceResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EventGrpcClient {

    // Must match the name in the application.yml
    @GrpcClient("event-service")
    private EventServiceGrpc.EventServiceBlockingStub eventStub;

    public BigDecimal fetchEventPrice(Long eventId) {
        GetEventPriceResponse response = eventStub.getEventPrice(
              GetEventPriceRequest.newBuilder()
                    .setEventId(eventId)
                    .build()
        );

        return new BigDecimal(response.getPrice());
    }
}