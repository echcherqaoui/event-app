package com.eventapp.paymentservice.config;

import com.eventapp.paymentservice.exception.domain.EventSecurityException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;

@Configuration
public class KafkaRetryConfig {

    @Bean
    public RetryTopicConfiguration retryTopicConfig(KafkaTemplate<String, Object> kafkaTemplate) {
        return RetryTopicConfigurationBuilder
              .newInstance()
              .maxAttempts(3)
              .fixedBackOff(5_000)
              .notRetryOn(EventSecurityException.class)
              .notRetryOn(TransientDataAccessException.class)
              .useSingleTopicForSameIntervals()
              .create(kafkaTemplate);
    }
}