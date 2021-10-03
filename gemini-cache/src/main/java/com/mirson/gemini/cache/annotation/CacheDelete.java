package com.mirson.gemini.cache.annotation;


import com.mirson.gemini.cache.utils.KeyGenerators;

import java.lang.annotation.*;

import static com.mirson.gemini.cache.utils.KeyGenerators.SHA;


/**
 * 缓存删除
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CacheDelete {

    /**
     * 缓存名称
     * @return
     */
    String[] cacheNames() default {};

    /**
     * 是否全部移除
     * @return
     */
    boolean removeAll() default false;

    /**
     * 缓存key（唯一性）
     * @return
     */
    String keyExpression() default "";

    /**
     * 是否异步
     * @return
     */
    boolean isAsync() default false;

    /**
     * 缓存key生成器
     * @return
     */
    KeyGenerators keyGenerator() default SHA;
}
