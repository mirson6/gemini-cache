package com.mirson.gemini.cache.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip压缩
 */
public class GenericGZIPRedisSerializer
        implements RedisSerializer<Object> {

    private static final Logger logger = LoggerFactory.getLogger(GenericGZIPRedisSerializer.class);

    public byte[] serialize(Object object) {
        ByteArrayOutputStream baos;
        GZIPOutputStream gzipOut;
        ObjectOutputStream objectOutputStream;
        try {
            if(null != object ) {
                baos = new ByteArrayOutputStream();
                gzipOut = new GZIPOutputStream(baos);
                objectOutputStream = new ObjectOutputStream(gzipOut);
                objectOutputStream.writeObject(object);
                objectOutputStream.flush();
                gzipOut.finish();
                objectOutputStream.close();
                return baos.toByteArray();
            }
            return null;
        } catch (Exception e) {
            throw new RedisSystemException(
                    "Could not serialize. ", e);
        }
    }

    public Object deserialize(byte[] bytes) {
        try {
            if(null != bytes) {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                GZIPInputStream gin = new GZIPInputStream(bais);
                ObjectInputStream ois = new ObjectInputStream(gin);
                return ois.readObject();
            }
            return null;
        } catch (Exception e) {
            if (e.getMessage().contains("InvalidClassException")) {
                throw new RedisSystemException(
                        "Could not deserialize. Invalid version of class found: ", e);
            } else {
                throw new RedisSystemException("Could not deserialize. ", e);
            }
        }
    }
}
