package com.mirson.gemini.cache.utils;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 *
 *
 * @author mirson
 * @date 2021/10/2
 */
@Configuration
public class SpringUtils implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(SpringUtils.class);

    private static ApplicationContext ctx = null;

    /**
     * 根据名称获取对象
     *
     * @param beanName
     * @return
     */
    public static Object getBean(String beanName) {
        if (ctx != null) {
            try {
                return ctx.getBean(beanName);
            } catch (Exception e) {
                logger.error("spring context getbean failure. " + e.getMessage(), e);
                return null;
            }
        }

        return null;
    }

    /**
     * 根据指定类型获取对象
     * @param requiredType
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> requiredType) {
        T result = null;
        if (ctx != null) {
            try {
                result = ctx.getBean(requiredType);
            } catch (Exception e) {
                logger.error("spring context getbean requiredType failure. " + e.getMessage(), e);
            }
        }

        return result;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }


}
