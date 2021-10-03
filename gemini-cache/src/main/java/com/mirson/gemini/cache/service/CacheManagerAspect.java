package com.mirson.gemini.cache.service;

import com.mirson.gemini.cache.annotation.CacheDelete;
import com.mirson.gemini.cache.annotation.CachePut;
import com.mirson.gemini.cache.annotation.Cacheable;
import com.mirson.gemini.cache.config.CacheConfigProperties;
import com.mirson.gemini.cache.service.cache.CacheService;
import com.mirson.gemini.cache.utils.CacheUtil;
import com.mirson.gemini.cache.utils.KeyGenerators;
import com.mirson.gemini.cache.utils.SpringExpressionParserUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * AOP切面，缓存拦截处理
 */
@Aspect
@Order(101)
@Component
@Slf4j
@ConditionalOnProperty(name = "app.cache.enable", havingValue = "true")
public class CacheManagerAspect {

    @Autowired
    SpringExpressionParserUtil springExpressionParserUtil;

    @Autowired
    CacheService redisCacheService;

    @Autowired
    private CacheConfigProperties cacheConfigProperties;

    @Pointcut("execution(* com.mirson..*.*(..)) && @annotation(com.mirson.gemini.cache.annotation.Cacheable)")
    public void executionOfCacheableMethod() {
    }

    @Pointcut("execution(* com.mirson..*.*(..)) && @annotation(com.mirson.gemini.cache.annotation.CachePut)")
    public void executionOfCachePutMethod() {
    }

    @Pointcut("execution(* com.mirson..*.*(..))  && @annotation(com.mirson.gemini.cache.annotation.CacheDelete)")
    public void executionOfCacheDeleteMethod() {
    }

    @AfterReturning(pointcut = "executionOfCachePutMethod()", returning = "returnObject")
    public void putInCache(final JoinPoint joinPoint, final Object returnObject) {

        try {
            if (returnObject == null) {
                return;
            }

            if (!cacheConfigProperties.isEnableCache()) {
                return;
            }
            CachePut cachePutAnnotation = getAnnotation(joinPoint, CachePut.class);
            Object cacheKey = springExpressionParserUtil
                    .parseAndGetCacheKeyFromExpression(cachePutAnnotation.keyExpression(), returnObject,
                            joinPoint.getArgs(), cachePutAnnotation.keyGenerator());

            if (cachePutAnnotation.isAsync()) {
                redisCacheService.saveInRedisAsync(cachePutAnnotation.cacheNames(), cacheKey, returnObject, cachePutAnnotation.TTL());
            } else {
                redisCacheService.save(cachePutAnnotation.cacheNames(), cacheKey, returnObject, cachePutAnnotation.TTL());
            }

        } catch (Exception e) {
            log.error("putInCache # Data save failed ## " + e.getMessage(), e);
        }
    }

    @AfterReturning(pointcut = "executionOfCacheDeleteMethod()", returning = "returnObject")
    public void deleteCache(final JoinPoint joinPoint, final Object returnObject) {

        try {
            if (!cacheConfigProperties.isEnableCache()) {
                return;
            }
            CacheDelete cacheDeleteAnnotation = getAnnotation(joinPoint, CacheDelete.class);

            String[] cacheNames = cacheDeleteAnnotation.cacheNames();
            Object cacheKey = null;
            if (!cacheDeleteAnnotation.removeAll()) {
                cacheKey = springExpressionParserUtil
                        .parseAndGetCacheKeyFromExpression(cacheDeleteAnnotation.keyExpression(), returnObject,
                                joinPoint.getArgs(), cacheDeleteAnnotation.keyGenerator());
            }
            if (cacheDeleteAnnotation.isAsync()) {
                if (cacheDeleteAnnotation.removeAll())
                    redisCacheService.invalidateCache(cacheNames);
            } else {
                redisCacheService.invalidateCache(cacheNames, cacheKey);
            }

        } catch (Exception e) {
            log.error("putInCache # Data delete failed! ## " + e.getMessage(), e);
        }
    }

    @Around("executionOfCacheableMethod()")
    public Object getAndSaveInCache(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        if (!cacheConfigProperties.isEnableCache()) {
            return callActualMethod(proceedingJoinPoint);
        }

        Object returnObject = null;

        Cacheable cacheableAnnotation = null;
        Object cacheKey = null;
        try {
            cacheableAnnotation = getAnnotation(proceedingJoinPoint, Cacheable.class);

            KeyGenerators keyGenerator = cacheableAnnotation.keyGenerator();

            if (StringUtils.isEmpty(cacheableAnnotation.keyExpression())) {
                cacheKey = CacheUtil.buildCacheKey(proceedingJoinPoint.getArgs());
            } else {
                cacheKey = springExpressionParserUtil
                        .parseAndGetCacheKeyFromExpression(cacheableAnnotation.keyExpression(), null,
                                proceedingJoinPoint.getArgs(), keyGenerator);
            }

            returnObject = redisCacheService.getFromCache(cacheableAnnotation.cacheName(), cacheKey);

        } catch (Exception e) {
            log.error("getAndSaveInCache # Redis op Exception while trying to get from cache ## " + e.getMessage(), e);
        }
        if (returnObject != null) {
            return returnObject;
        } else {
            returnObject = callActualMethod(proceedingJoinPoint);

            if (returnObject != null) {
                try {
                    if (cacheableAnnotation.isAsync()) {
                        redisCacheService
                                .saveInRedisAsync(new String[]{cacheableAnnotation.cacheName()}, cacheKey,
                                        returnObject, cacheableAnnotation.TTL());
                    } else {
                        redisCacheService
                                .save(new String[]{cacheableAnnotation.cacheName()}, cacheKey,
                                        returnObject, cacheableAnnotation.TTL());
                    }
                } catch (Exception e) {
                    log.error("getAndSaveInCache # Exception occurred while trying to save data in redis##" + e.getMessage(),
                            e);
                }
            }
        }
        return returnObject;
    }

    private Object callActualMethod(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        return proceedingJoinPoint.proceed();

    }

    private <T extends Annotation> T getAnnotation(JoinPoint proceedingJoinPoint,
                                                   Class<T> annotationClass) throws NoSuchMethodException {

        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        if (method.getDeclaringClass().isInterface()) {
            method = proceedingJoinPoint.getTarget().getClass().getDeclaredMethod(methodName,
                    method.getParameterTypes());
        }
        return method.getAnnotation(annotationClass);
    }

}
