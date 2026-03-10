package com.eventapp.bookingservice.config;

import com.eventapp.contracts.booking.v1.BookingConfirmed;
import com.eventapp.contracts.booking.v1.RefundRequested;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProtobufConfig {

    @Value("${app.schema-registry.url}")
    private String schemaRegistryUrl;

    private <T extends com.google.protobuf.Message> KafkaProtobufSerializer<T> createSerializer() {
        KafkaProtobufSerializer<T> serializer = new KafkaProtobufSerializer<>();
        Map<String, Object> config = new HashMap<>();
        config.put("schema.registry.url", schemaRegistryUrl);
        config.put("auto.register.schemas", "false");
        config.put("use.latest.version", "true");

        serializer.configure(config, false);

        return serializer;
    }

    @Bean
    public KafkaProtobufSerializer<RefundRequested> refundSerializer() {
        return createSerializer();
    }

    @Bean
    public KafkaProtobufSerializer<BookingConfirmed> bookingConfirmedSerializer() {
        return createSerializer();
    }
}