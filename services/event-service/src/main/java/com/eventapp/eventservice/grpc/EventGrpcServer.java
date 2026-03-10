package com.eventapp.eventservice.grpc;

import com.eventapp.eventservice.service.IEventService;
import com.eventapp.lib.event.v1.EventServiceGrpc;
import com.eventapp.lib.event.v1.GetEventPriceRequest;
import com.eventapp.lib.event.v1.GetEventPriceResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;

@GrpcService
@RequiredArgsConstructor
public class EventGrpcServer extends  EventServiceGrpc.EventServiceImplBase {
    
    private final IEventService eventService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public void getEventPrice(GetEventPriceRequest request,
                              StreamObserver<GetEventPriceResponse> responseObserver) {
        long eventId = request.getEventId();

        BigDecimal price = eventService.getPriceById(eventId);

        // Map Entity to gRPC Response
        GetEventPriceResponse response = GetEventPriceResponse.newBuilder()
              .setEventId(eventId)
              .setPrice(price.toString())
              .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}