package com.wt.maven.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @author yipin
 * @date 2020/11/5
 */
@Slf4j
public class JacksonUtil {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String writeValueAsString(Object value) throws RuntimeException {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            String msg = String.format("Jackson转String失败,对象:{}", value);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static byte[] writeValueAsBytes(Object value) throws RuntimeException {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            String msg = String.format("Jackson转String失败,对象:{}", value);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static <T> T readValue(String content, Class<T> valueType) throws RuntimeException {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            String msg = String.format("Jackson转换对象失败,String:{},Class:{}", content, valueType);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static <T> T readValue(byte[] content, Class<T> valueType) throws RuntimeException {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (IOException e) {
            String msg = null;
            try {
                msg = String.format("Jackson转换对象失败,String:{},Class:{}", new String(content, "utf-8"), valueType);
            } catch (UnsupportedEncodingException ue) {
                log.error("byte数组转String失败", ue);
            }
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static <T> T readValue(String content, TypeReference<T> valueTypeRef) throws RuntimeException {
        try {
            return objectMapper.readValue(content, valueTypeRef);
        } catch (JsonProcessingException e) {
            String msg = String.format("Jackson转换对象失败,String:{},Class:{}", content, valueTypeRef);
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

}
