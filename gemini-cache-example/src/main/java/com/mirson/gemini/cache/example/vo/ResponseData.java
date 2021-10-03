package com.mirson.gemini.cache.example.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 *
 * @author mirson
 * @date 2021/10/3
 */
@Data
public class ResponseData<T> implements Serializable {
    /**
     * 返回响应码
     */
    private int code;
    /**
     * 返回描述
     */
    private String msg;
    /**
     * 返回结果
     */
    private T data;

    /**
     * 响应时间
     */
    private Date respTime;


}
