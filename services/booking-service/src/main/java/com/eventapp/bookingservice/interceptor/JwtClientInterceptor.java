package com.eventapp.bookingservice.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@GrpcGlobalClientInterceptor // Applies to all outgoing gRPC calls
public class JwtClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> AUTH_HEADER =
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <I, O> ClientCall<I, O> interceptCall(MethodDescriptor<I, O> method,
                                                 CallOptions options, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, options)) {
            @Override
            public void start(Listener<O> responseListener,
                              Metadata headers) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (!(authentication instanceof JwtAuthenticationToken jwtAuth))
                    throw Status.UNAUTHENTICATED
                          .withDescription("Missing JWT in SecurityContext")
                          .asRuntimeException();

                String token = jwtAuth.getToken().getTokenValue();
                headers.put(AUTH_HEADER, "Bearer " + token);

                super.start(responseListener, headers);
            }
        };
    }
}