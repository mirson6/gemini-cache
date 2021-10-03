package com.mirson.gemini.cache.example.service.impl;

import com.mirson.gemini.cache.annotation.Cacheable;
import com.mirson.gemini.cache.example.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 
 *
 * @author mirson
 * @date 2021/10/3
 */
@Service
@Slf4j
public class OrderServiceImpl implements IOrderService {


    /**
     * 根据订单编号获取订单信息
     * @param orderNo
     * @return
     */
    @Cacheable(cacheName = "gemini_cache_order", keyExpression = "#param1", TTL = 10)
    @Override
    public String getOrder(String orderNo){
        log.info("enter getOrder method, orderNo: " + orderNo);
        return "get order, orderNo: " + orderNo;
    }


}
