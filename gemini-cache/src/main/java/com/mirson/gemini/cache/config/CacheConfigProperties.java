package com.mirson.gemini.cache.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 
 *
 * @author mirson
 * @date 2021/9/26
 */
@Component
@Data
@ConditionalOnProperty(name = "app.cache.enable", havingValue = "true")
public class CacheConfigProperties {

    /**
     * 系统唯一标识
     */
    public static final String SYSTEM_ID = UUID.randomUUID().toString();

    /**
     * 是否开启缓存
     */
    @Value("${app.cache.enable}")
    private boolean enableCache;

    /**
     * 是否开启二级缓存
     */
    @Value("${app.cache.enableSecondCache: false}")
    private boolean enableSecondCache;

    /**
     * 最大连接数（默认为16）
     */
    @Value("${app.cache.redis.executor.maxPoolSize: 16}")
    private int executorMaxSize;

    /**
     *  核心连接数（默认为8）
     */
    @Value("${app.cache.redis.executor.corePoolSize: 8}")
    private int executorCoreSize;

    /**
     * 线程空闲存活有效时间（单位：秒， 默认30秒）
     */
    @Value("${app.cache.redis.executor.aliveTime: 30}")
    private int executorAliveTime;

    /**
     * 线程池队列大小（默认1000）
     */
    @Value("${app.cache.redis.executor.queueCapacity: 1000}")
    private int executorQueueCapacity;

    /**
     * 连接池最大连接数（默认100）
     */
    @Value("${app.cache.redis.pool.maxSize: 100}")
    private int poolMaxSize;

    /**
     * 连接池最小空闲连接数(默认5）
     */
    @Value("${app.cache.redis.pool.minIdleSize: 5}")
    private int minIdleSize;

    /**
     * 连接池超时时间（单位：毫秒， 包括连接与读取超时时间）
     */
    @Value("${app.cache.redis.pool.timeout: 3000}")
    private int timeout;


    /**
     *  Redis主机连接信息
     */
    @Value("${app.cache.redis.host:#{null}}")
    private String host;

    /**
     * Redis 全局缓存超时时间， 单位：秒，默认为24小时
     */
    @Value("${app.cache.redis.global.expire: 24*3600 }")
    private long expire;
    /**
     *  Redis 集群连接信息
     */
    @Value("${app.cache.redis.clusterNodes:#{null}}")
    private String clusterNodes;

    /**
     * Redis连接密码
     */
    @Value("${app.cache.redis.password}")
    private String password;

    /**
     * Redis 主机端口
     */
    @Value("${app.cache.redis.port}")
    private int port;

    /**
     * 数据库编号
     */
    @Value("${app.cache.redis.database}")
    private int database;

    /**
     * 是否开启存储压缩
     */
    @Value("${app.cache.redis.useCompression}")
    private boolean useCompression;

    /**
     * 分布式缓存更新的的topic名称
     */
    @Value("${app.cache.caffeine.topic: cache:redis:caffeine:topic}")
    private String topic;


    /**
     * 访问后过期时间，单位毫秒
     */
    @Value("${app.cache.caffeine.expireAfterAccess:0}")
    private long expireAfterAccess;

    /**
     * 写入后过期时间，单位毫秒
     */
    @Value("${app.cache.caffeine.expireAfterWrite:0}")
    private long expireAfterWrite;

    /**
     * 写入后刷新时间，单位毫秒
     */
    @Value("${app.cache.caffeine.refreshAfterWrite:0}")
    private long refreshAfterWrite;

    /**
     * 初始化大小
     */
    @Value("${app.cache.caffeine.initialCapacity:0}")
    private int initialCapacity;

    /**
     * 最大缓存对象个数，超过此数量时之前放入的缓存将失效
     */
    @Value("${app.cache.caffeine.maximumSize:0}")
    private long maximumSize;

}
