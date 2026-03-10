package com.eventapp.paymentservice.config;

import com.eventapp.contracts.payment.v1.PaymentEvent;
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

    @Bean(destroyMethod = "close")
    public KafkaProtobufSerializer<PaymentEvent> paymentSerializer() {
        KafkaProtobufSerializer<PaymentEvent> serializer = new KafkaProtobufSerializer<>();

        Map<String, Object> config = new HashMap<>();
        config.put("schema.registry.url", schemaRegistryUrl);
        config.put("auto.register.schemas", "false");
        // Ensures using the latest schema version
        config.put("use.latest.version", "true");

        serializer.configure(config, false);

        return serializer;
    }
}