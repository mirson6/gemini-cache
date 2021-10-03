package com.mirson.gemini.cache.service.cache;

import com.mirson.gemini.cache.config.CacheConfigProperties;
import org.redisson.api.RBatch;
import org.redisson.api.RMapCache;
import org.redisson.api.RMapCacheAsync;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务管理实现类
 */
public class RedisCacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheServiceImpl.class);

    private ExecutorService serviceCallExecutorService;

    private RedissonClient redissonClient;

    private CacheConfigProperties cacheConfigProperties;

    /**
     * 初始化
     */
    public RedisCacheServiceImpl(RedissonClient redissonClient,
                                 ExecutorService serviceCallExecutorService,
                                 CacheConfigProperties cacheConfigProperties) {
        this.redissonClient = redissonClient;
        this.serviceCallExecutorService = serviceCallExecutorService;
        this.cacheConfigProperties = cacheConfigProperties;
    }


    /**
     * 获取缓存对象
     * @param cacheName
     * @param cacheKey
     * @return
     */
    @Override
    public Object getFromCache(final String cacheName, final Object cacheKey) {
        if (StringUtils.isEmpty(cacheName) || cacheKey == null) {
            throw new IllegalArgumentException("Cache name or cache key can not be null!");
        }

        return redissonClient.getMapCache(cacheName).get(cacheKey);
    }

    /**
     * 保存至REDIS缓存
     * @param cacheNames
     * @param cacheKey
     * @param cacheValue
     * @return
     */
    @Override
    public boolean save(final String[] cacheNames, final Object cacheKey,
                        final Object cacheValue, final long ttl) {

        // 校验判断
        if (cacheNames == null || cacheNames.length == 0) {
            throw new IllegalArgumentException(
                    "Cache names list can not be null or empty for save operation!!");
        }

        for(String cacheName : cacheNames) {
            RMapCache mapCache = redissonClient.getMapCache(cacheName);
            boolean isExists = mapCache.isExists();
            if(!isExists) {
                // 第一次保存， 并设定超时时间
                firstSave(cacheName, cacheKey, cacheValue, ttl);
            }else {
                mapCache.put(cacheKey, cacheValue, ttl, TimeUnit.SECONDS);
            }
        }
        return true;
    }

    private void firstSave(String cacheName, final Object cacheKey,
                           final Object cacheValue, final long ttl) {
        RBatch batch = redissonClient.createBatch();
        RMapCacheAsync rMapCacheAsync = batch.getMapCache(cacheName);
        rMapCacheAsync.putAsync(cacheKey, cacheValue, ttl, TimeUnit.SECONDS);
        rMapCacheAsync.expireAsync(cacheConfigProperties.getExpire(), TimeUnit.SECONDS);
        batch.execute();
    }

    /**
     * 清理缓存
     * @param cacheNames
     * @param cacheKey
     * @return
     */
    @Override
    public boolean invalidateCache(final String[] cacheNames, final Object cacheKey) {

        if (cacheNames == null || cacheNames.length == 0) {
            throw new IllegalArgumentException(
                    "Cache names list can not be null or empty for save operation!!");
        }
        for (String cacheName : cacheNames) {
            if (StringUtils.isEmpty(cacheName)) {
                continue;
            }
            if (null == redissonClient.getMapCache(cacheName)) {
                continue;
            }
            redissonClient.getMapCache(cacheName).remove(cacheKey);
        }
        return true;
    }

    /**
     * 清理缓存
     * @param cacheNames
     * @return
     */
    @Override
    public boolean invalidateCache(final String[] cacheNames) {
        for (String cacheName : cacheNames) {
            redissonClient.getMapCache(cacheName).delete();
        }
        return true;
    }

    @Override
    public boolean saveInRedisAsync(final String[] cacheNames, final Object cacheKey,
                                    final Object cacheValue, long ttl) {

        // 异步线程池执行处理
        serviceCallExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                save(cacheNames, cacheKey, cacheValue, ttl);
            }
        });
        return true;
    }

    /**
     *  清除缓存信息（异步方式）
     * @param cacheNames
     * @param cacheKey
     * @return
     */
    @Override
    public boolean invalidateCacheAsync(final String[] cacheNames, final Object cacheKey) {

        serviceCallExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                invalidateCache(cacheNames, cacheKey);
            }
        });

        return true;
    }

    /**
     * 异步方式清理缓存
     * @param cacheNames
     * @return
     */
    @Override
    public boolean invalidateCacheAsync(final String[] cacheNames) {
        serviceCallExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                invalidateCache(cacheNames);
            }
        });
        return true;
    }

}
