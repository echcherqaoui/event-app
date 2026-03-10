package com.eventapp.bookingservice.config;

import com.eventapp.bookingservice.exceptions.domain.EventSecurityException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
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
              .maxAttempts(4)
              .fixedBackOff(5_000)
              .notRetryOn(EventSecurityException.class)
              .notRetryOn(TransientDataAccessException.class)
              .notRetryOn(RecoverableDataAccessException.class)
              .useSingleTopicForSameIntervals() // single retry topic for fixed intervals
              .create(kafkaTemplate);
    }
}