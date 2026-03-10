package com.eventapp.notificationservice.kafka.deserializer;

import com.eventapp.contracts.booking.v1.BookingConfirmed;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.DeserializationException;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtobufDeserializerTest {

    private ProtobufDeserializer<BookingConfirmed> deserializer;
    private static final String TOPIC = "test-topic";

    @BeforeEach
    void setUp() {
        deserializer = new ProtobufDeserializer<>();
        Map<String, Object> configs = new HashMap<>();
        configs.put("specific.protobuf.value.type", BookingConfirmed.class.getName());

        deserializer.configure(configs, false);
    }

    private byte[] createConfluentProtobufBytes(BookingConfirmed message) {
        byte[] payload = message.toByteArray();
        // 1 (magic) + 4 (schemaId) + 1 (index 0 as varint) + payload
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 1 + payload.length);
        buffer.put((byte) 0);      // Magic byte
        buffer.putInt(123);        // Fake Schema ID
        buffer.put((byte) 0);      // Message Index 0 (Varint)
        buffer.put(payload);

        return buffer.array();
    }

    @Nested
    @DisplayName("Success Cases")
    class SuccessTests {
        @Test
        @DisplayName("Should successfully deserialize a valid Confluent-formatted message")
        void shouldDeserialize_WhenMessageFormatIsValid() {
            BookingConfirmed original = BookingConfirmed.newBuilder()
                  .setBookingId("BOOK-1")
                  .setUserEmail("test@test.com")
                  .build();

            byte[] data = createConfluentProtobufBytes(original);

            BookingConfirmed result = deserializer.deserialize(TOPIC, data);

            assertThat(result).isNotNull();
            assertThat(result.getBookingId()).isEqualTo("BOOK-1");
            assertThat(result.getUserEmail()).isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("Should return null when data is null or empty")
        void shouldReturnNull_WhenDataIsNullOrEmpty() {
            assertThat(deserializer.deserialize(TOPIC, null)).isNull();
            assertThat(deserializer.deserialize(TOPIC, new byte[0])).isNull();
        }
    }

    @Nested
    @DisplayName("Wire Format Validation (getCodedInputStream)")
    class WireFormatTests {
        @Test
        @DisplayName("Should throw exception if magic byte is not zero")
        void shouldThrowException_WhenMagicByteIsNotZero() {
            byte[] data = new byte[]{1, 0, 0, 0, 5, 0, 1, 2}; // Magic byte = 1

            assertThatThrownBy(() -> deserializer.deserialize(TOPIC, data))
                  .isInstanceOf(DeserializationException.class)
                  .hasCauseInstanceOf(IllegalArgumentException.class)
                  .hasStackTraceContaining("Not a Confluent Protobuf message");
        }

        @Test
        @DisplayName("Should throw exception if data is too short for schema ID")
        void shouldThrowException_WhenDataIsTooShortForSchemaId() {
            byte[] data = new byte[]{0, 0, 0}; // Only 3 bytes

            assertThatThrownBy(() -> deserializer.deserialize(TOPIC, data))
                  .isInstanceOf(DeserializationException.class)
                  .hasStackTraceContaining("Data too short for schema ID");
        }

        @Test
        @DisplayName("Should handle multi-byte Varint message indexes")
        void shouldHandleMultiByteVarint_WhenMessageIndexIsLarge() {
            // Index 300 in Varint is [0xAC, 0x02]
            byte[] payload = BookingConfirmed.newBuilder()
                  .setBookingId("ID")
                  .build()
                  .toByteArray();

            ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 2 + payload.length);

            buffer.put((byte) 0)
                  .putInt(1)
                  .put((byte) 0xAC)
                  .put((byte) 0x02)
                  .put(payload);

            BookingConfirmed result = deserializer.deserialize(TOPIC, buffer.array());
            assertThat(result.getBookingId()).isEqualTo("ID");
        }
    }

    @Nested
    @DisplayName("Configuration & Reflection")
    class ConfigTests {
        @Test
        @DisplayName("Should throw exception if config property is missing")
        void shouldThrowException_WhenConfigPropertyIsMissing() {
            // Initialize outside the assertion to avoid try-with-resources warnings
            Map<String, Object> emptyConfigs = new HashMap<>();

            try (ProtobufDeserializer<BookingConfirmed> d = new ProtobufDeserializer<>()) {
                ThrowableAssert.ThrowingCallable call = () -> d.configure(emptyConfigs, false);

                assertThatThrownBy(call)
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("missing");
            }
        }

        @Test
        @DisplayName("Should throw exception if class name is invalid")
        void shouldThrowException_WhenProtobufClassNameIsInvalid() {
            Map<String, Object> configs = Map.of("specific.protobuf.value.type", "com.fake.MissingClass");
            assertThatThrownBy(() -> deserializer.configure(configs, false))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("not found");
        }
    }
}