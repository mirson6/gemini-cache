package com.mirson.gemini.cache.example.startup; /**
 * 
 *
 * @author mirson
 * @date 2021/10/3
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


/**
 * 缓存示例启动程序
 */
@SpringBootApplication
@ComponentScan(basePackages ="com.mirson")
public class GeminiCacheExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeminiCacheExampleApplication.class, args);
    }

}
