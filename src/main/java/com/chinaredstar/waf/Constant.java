package com.chinaredstar.waf;

import com.chinaredstar.waf.config.IpRateConf;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author:杨果
 * @date:2017/4/11 下午1:52
 *
 * Description:
 *
 */
public class Constant {
    static {
        ApplicationContext factory = new ClassPathXmlApplicationContext("classpath:spring/applicationContext-*.xml");
        IpRateConf ipRateConf = (IpRateConf) factory.getBean("ipRateConf");
        RedisTemplate cacheRedisTemplate = (RedisTemplate) factory.getBean("cacheRedisTemplate");
        ipRateUtil = new IpRateUtil(ipRateConf, cacheRedisTemplate);
    }

    public static IpRateUtil ipRateUtil;
    public static int acceptorThreads = 5;
    public static int clientToProxyWorkerThreads = 20;
    public static int proxyToServerWorkerThreads = 20;
    public static int serverPort=8888;
}
