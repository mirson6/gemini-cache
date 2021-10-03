package com.mirson.gemini.cache.service.cache;

import com.mirson.gemini.cache.config.CacheConfigProperties;
import com.mirson.gemini.cache.service.listener.CacheMessage;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Redis发送服务实现接口
 *
 * @author mirson
 * @date 2021/9/26
 */
public class RedisSendServiceImpl implements RedisSendService {

    private static final Logger logger = LoggerFactory.getLogger(RedisSendServiceImpl.class);

     /**
      * 缓存配置属性
      */
     private CacheConfigProperties cacheConfigProperties;

     /**
      * Redis操作接口
      */
     private RedissonClient redissonClient;

     public RedisSendServiceImpl(CacheConfigProperties cacheConfigProperties,
                                 RedissonClient redissonClient){
         this.cacheConfigProperties = cacheConfigProperties;
         this.redissonClient = redissonClient;
     }
    /**
     * 发送缓存变更消息
     * @param cacheNames
     */
    @Override
    public void sendMessage(String[] cacheNames) {
        sendMessage(cacheNames, null);
    }

    /**
     * 发送缓存变更消息
     * @param cacheName
     */
    @Override
    public void sendMessage(String cacheName) {
        sendMessage(new String[]{cacheName}, null);
    }

    /**
     * 发送缓存变更消息
     * @param cacheName
     * @param key
     */
    @Override
    public void sendMessage(String cacheName, Object key) {
        sendMessage(new String[]{cacheName}, key);
    }

    /**
     * 发送缓存变更消息
     * @param cacheNames
     * @param key
     */
    @Override
    public void sendMessage(String[] cacheNames, Object key) {
        RTopic rTopic = redissonClient.getTopic(cacheConfigProperties.getTopic());
        long receive = rTopic.publish(new CacheMessage(cacheNames, key));
        logger.info("sendMessage receive clients: " + receive);
    }

}
