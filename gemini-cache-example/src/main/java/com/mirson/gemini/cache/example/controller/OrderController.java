package com.mirson.gemini.cache.example.controller;

import com.mirson.gemini.cache.example.service.IOrderService;
import com.mirson.gemini.cache.example.vo.ResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

/**
 * 
 *
 * @author mirson
 * @date 2021/10/3
 */
@Controller
public class OrderController {

    /**
     * 订单服务接口
     */
    @Autowired
    private IOrderService orderService;

    /**
     * 获取订单信息
     * @param orderNo
     * @return
     */
    @RequestMapping(value = "/getOrder")
    @ResponseBody
    public ResponseData getOrder(String orderNo) {
        ResponseData respData = new ResponseData();
        respData.setCode(200);
        respData.setData(orderService.getOrder(orderNo));
        respData.setRespTime(new Date());
        return respData;
    }
}
