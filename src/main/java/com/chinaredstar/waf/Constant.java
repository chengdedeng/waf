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
        IpRateUtil = new IpRateUtil(ipRateConf, cacheRedisTemplate);
    }

    enum X_Frame_Options {
        DENY,//表示该页面不允许在 frame 中展示,即便是在相同域名的页面中嵌套也不允许.
        SAMEORIGIN//表示该页面可以在相同域名页面的 frame 中展示.
    }

    public static IpRateUtil IpRateUtil;
    public static int AcceptorThreads = 5;
    public static int ClientToProxyWorkerThreads = 20;
    public static int ProxyToServerWorkerThreads = 20;
    public static int ServerPort = 8888;
    public static RedStarHostResolver RedStarHostResolver = new RedStarHostResolver();
    public static X_Frame_Options X_Frame_Option = X_Frame_Options.SAMEORIGIN;
    public static String hugeFilePattern="\\.iso$|\\.dmg$|\\.mp4$|\\.mp3$|\\.avi$|\\.exe$";
}
