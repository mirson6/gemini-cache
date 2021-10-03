package com.mirson.gemini.cache.annotation;


import com.mirson.gemini.cache.utils.KeyGenerators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.mirson.gemini.cache.utils.KeyGenerators.SHA;


/**
 * 新增缓存
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {

    /**
     * 缓存名称
     * @return
     */
    String cacheName() default "";

    /**
     * 缓存生命周期 (单位：秒）
     * @return
     */
    long TTL() default 0;

    /**
     * 缓存KEY(唯一性）
     * @return
     */
    String keyExpression() default "";

    /**
     * 是否异步
     * @return
     */
    boolean isAsync() default false;

    /**
     * key生成器
     * @return
     */
    KeyGenerators keyGenerator() default SHA;

}