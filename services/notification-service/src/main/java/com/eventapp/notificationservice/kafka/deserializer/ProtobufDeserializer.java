package com.eventapp.notificationservice.kafka.deserializer;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Message;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Map;

public class ProtobufDeserializer<T extends Message> implements Deserializer<T> {
    private Method parseMethod;

    private CodedInputStream getCodedInputStream(byte[] data) {
        int offset = 0;

        // magic byte
        if (data[0] != 0) throw new IllegalArgumentException("Not a Confluent Protobuf message");

        offset += 1;

        // schema ID (4 bytes)
        if (data.length < 5) throw new IllegalArgumentException("Data too short for schema ID");

        offset += 4;

        // decode varint message index
        int shift = 0;
        while (true) {
            if (offset >= data.length) throw new IllegalArgumentException("Unexpected end of data while reading message index");

            int b = data[offset++] & 0xFF;
            if ((b & 0x80) == 0) break;
            shift += 7;
            if (shift > 28) throw new IllegalArgumentException("Varint too long");
        }

        // offset now points to the start of the actual protobuf payload
        return CodedInputStream.newInstance(
              data,
              offset,
              data.length - offset
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, ?> configs, boolean isKey) {
        String typeName = (String) configs.get("specific.protobuf.value.type");
        if (typeName == null || typeName.isBlank())
            throw new IllegalArgumentException("Kafka property 'specific.protobuf.value.type' is missing.");

        try {
            Class<T> targetType = (Class<T>) ClassUtils.forName(typeName, null);
            // Cache the method once during startup
            this.parseMethod = targetType.getMethod(
                  "parseFrom",
                  CodedInputStream.class
            );
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Protobuf class not found: " + typeName, e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                  "Class " + typeName + " does not have a static parseFrom(CodedInputStream) method", e
            );
        } catch (Exception e) {
            // Fallback for unexpected reflection issues
            throw new SerializationException(
                  "Failed to initialize ProtobufDeserializer for " + typeName, e
            );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            CodedInputStream inputStream = getCodedInputStream(data);
            return (T) parseMethod.invoke(null, inputStream);
        } catch (Exception e) {
            throw new DeserializationException(
                  "Failed to deserialize Protobuf message from topic: " + topic,
                  data,
                  false,
                  e
            );
        }
    }
}