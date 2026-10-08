package com.finovara.activitylogservice.config.redis;

import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.ArrayList;
import java.util.List;

public record ListSafeRedisSerializer(RedisSerializer<Object> delegate) implements RedisSerializer<Object> {

    @Override
    public byte[] serialize(Object value) {
        return delegate.serialize(value instanceof List<?> list ? new ArrayList<>(list) : value);
    }

    @Override
    public Object deserialize(byte[] bytes) {
        return delegate.deserialize(bytes);
    }
}