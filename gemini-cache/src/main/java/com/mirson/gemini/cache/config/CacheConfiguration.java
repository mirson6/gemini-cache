package com.mirson.gemini.cache.config;

import com.mirson.gemini.cache.service.cache.*;
import com.mirson.gemini.cache.service.listener.CacheMessageListener;
import com.mirson.gemini.cache.utils.SpringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.FstCodec;
import org.redisson.codec.LZ4Codec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  缓存配置
 */
@Configuration
@EnableAspectJAutoProxy
@ConditionalOnProperty(name = "app.cache.enable", havingValue = "true")
@Order(10)
public class CacheConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfiguration.class);

    /**
     * 缓存参数配置
     */
    @Autowired
    private CacheConfigProperties cacheConfigProperties;

    /**
     * 线程池等待结束时间
     */
    private static final int awaitTerminationSeconds = 60;

    /**
     * 线程池配置
     * @return
     */
    @Bean
    public ExecutorService redisExecutor() {

        return new ThreadPoolExecutor(
                cacheConfigProperties.getExecutorCoreSize(),
                cacheConfigProperties.getExecutorMaxSize(),
                cacheConfigProperties.getExecutorAliveTime(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(cacheConfigProperties.getExecutorQueueCapacity()),
                new NamedThreadFactory("Redisson-Pool"));
    }

    /**
     * 增加关闭钩子处理， 实现Redisson线程池优雅关闭
     */
    @PostConstruct
    public void addShutdown(){
        Object redisExecutorObj = SpringUtils.getBean("redisExecutor");
        if(null != redisExecutorObj) {
            ExecutorService redisExecutor = (ExecutorService)redisExecutorObj;
            // 关闭钩子处理
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    redisExecutor.shutdown();
                    try {
                        if (!redisExecutor.awaitTermination(awaitTerminationSeconds, TimeUnit.SECONDS)) {
                            logger.info("Redisson Pool Executor did not terminate in the specified time.");
                            List<Runnable> droppedTasks = redisExecutor.shutdownNow();
                            logger.info("Redisson Pool Executor was abruptly shutdown. " + droppedTasks.size() + " tasks will not be executed."); //optional **
                        }
                    } catch (InterruptedException e) {
                        logger.error("Redisson Pool Executor shutdown Error: " + e.getMessage(), e);
                    }
                }
            });
        }
    }

    @Bean
    public RedissonClient redissonClient(ExecutorService redisExecutor){
        Config config = new Config();
        RedissonClient redisson = null;
        if(null != cacheConfigProperties.getHost()) {
            // 单机连接方式
            SingleServerConfig serverConfig = config.useSingleServer().setAddress("redis://" + cacheConfigProperties.getHost() + ":" + cacheConfigProperties.getPort());
            serverConfig.setDatabase(cacheConfigProperties.getDatabase());
            serverConfig.setPassword(cacheConfigProperties.getPassword());
            serverConfig.setConnectionMinimumIdleSize(cacheConfigProperties.getMinIdleSize());
            serverConfig.setConnectionPoolSize(cacheConfigProperties.getPoolMaxSize());
            serverConfig.setConnectTimeout(cacheConfigProperties.getTimeout());
            serverConfig.setTimeout(cacheConfigProperties.getTimeout());
            redisson = Redisson.create(config);
        }else {
            if(null == cacheConfigProperties.getClusterNodes()) {
                throw new RuntimeException("You need to config the clusterNodes property!");
            }
            // 集群连接方式
            ClusterServersConfig serversConfig = config.useClusterServers();
            serversConfig.setConnectTimeout(cacheConfigProperties.getTimeout());
            serversConfig.setTimeout(cacheConfigProperties.getTimeout());
            serversConfig.setMasterConnectionPoolSize(cacheConfigProperties.getPoolMaxSize());
            serversConfig.setSlaveConnectionPoolSize(cacheConfigProperties.getPoolMaxSize());
            serversConfig.setMasterConnectionMinimumIdleSize(cacheConfigProperties.getMinIdleSize());
            serversConfig.setSlaveConnectionMinimumIdleSize(cacheConfigProperties.getMinIdleSize());

            Arrays.stream(cacheConfigProperties.getClusterNodes().split(",")).forEach(host->serversConfig.addNodeAddress("redis://"+ host.trim()));
            serversConfig.setPassword(cacheConfigProperties.getPassword());
            redisson = Redisson.create(config);
        }
        redisson.getConfig().setExecutor(redisExecutor);
        if(cacheConfigProperties.isUseCompression()) {
            // 开启压缩, 采用LZ4压缩
            redisson.getConfig().setCodec(new LZ4Codec());
        }else {
            // 高速序列化编码
            redisson.getConfig().setCodec(new FstCodec());

        }
        return redisson;
    }




    /**
     * Redis缓存更新消息发送接口
     * @param cacheConfigProperties
     * @param redissonClient
     * @return
     */
    @Bean
    public RedisSendService redisSendService(CacheConfigProperties cacheConfigProperties,
                                             RedissonClient redissonClient) {
        RedisSendService redisSendService = new RedisSendServiceImpl(cacheConfigProperties, redissonClient);
        return redisSendService;
    }

    /**
     * 缓存服务实现接口
     * @return
     */
    @Bean
    public CacheService cacheService(RedissonClient redissonClient,
                                     RedisSendService redisSendService,
                                     ExecutorService redisExecutor) {
        CacheService cacheService = null;
        // 判断是否开启二级缓存
        if(cacheConfigProperties.isEnableSecondCache()) {
            CacheService redisCacheService = new RedisCacheServiceImpl(redissonClient, redisExecutor, cacheConfigProperties);
            cacheService = new CaffeineCacheServiceImpl(redisCacheService, redisSendService, cacheConfigProperties);
        }else {
            cacheService = new RedisCacheServiceImpl(redissonClient, redisExecutor, cacheConfigProperties);
        }
        return cacheService;
    }


    /**
     * 设置消息监听器
     * @param redissonClient
     * @param caffeineCacheService
     * @return
     */
    @ConditionalOnProperty(
            value = "app.cache.enableSecondCache",
            havingValue = "true")
    @Bean
    public RTopic subscribe(RedissonClient redissonClient, CacheService caffeineCacheService) {
        RTopic rTopic = redissonClient.getTopic(cacheConfigProperties.getTopic());
        CacheMessageListener messageListener = new CacheMessageListener((CaffeineCacheServiceImpl)caffeineCacheService);
        rTopic.addListener(messageListener);
        return rTopic;
    }

}
