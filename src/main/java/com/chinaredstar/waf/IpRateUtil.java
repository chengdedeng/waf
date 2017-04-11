package com.chinaredstar.waf;

import com.chinaredstar.waf.config.IpRateConf;

import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author:杨果
 * @date:2017/4/6 下午7:24
 *
 * Description:
 *
 */
public class IpRateUtil {
    private static Logger logger = LoggerFactory.getLogger(IpRateUtil.class);
    private IpRateConf ipRateConfClone;
    private IpRateConf ipRateConf;
    private RedisTemplate redisTemplate;

    private ConcurrentHashMap<String, Long> sRate = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Long> mRate = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Long> hRate = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Long> dRate = new ConcurrentHashMap<String, Long>();


    public IpRateUtil(IpRateConf ipRateConf, RedisTemplate redisTemplate) {
        this.ipRateConfClone = (IpRateConf) ipRateConf.clone();
        this.ipRateConf = ipRateConf;
        this.redisTemplate = redisTemplate;
        changeConf(ipRateConf.getS(), sRate);
        changeConf(ipRateConf.getM(), mRate);
        changeConf(ipRateConf.getH(), hRate);
        changeConf(ipRateConf.getD(), dRate);
    }

    private void changeConf(String strConf, ConcurrentHashMap<String, Long> mapConf) {
        HashMap<String, Long> tmp = new HashMap<>();
        String[] confs = strConf.split(",");
        for (String conf : confs) {
            if (conf != null && !"".equals(conf)) {
                String[] kv = conf.split(":");
                String uri = kv[0];
                Long rate = Long.parseLong(kv[1]);
                tmp.put(uri, rate);
            }
        }
        for (ConcurrentHashMap.Entry<String, Long> entry : mapConf.entrySet()) {
            if (!tmp.containsKey(entry.getKey())) {
                mapConf.remove(entry.getKey());
            }
        }
        for (HashedMap.Entry<String, Long> entry : tmp.entrySet()) {
            mapConf.put(entry.getKey(), entry.getValue());
        }
    }

    private void addKey(final String key, final long expire) {
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            public List execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                BoundListOperations boundListOperations = operations.boundListOps(key);
                boundListOperations.rightPush(1);
                boundListOperations.expire(expire, TimeUnit.SECONDS);
                List result = operations.exec();
                return result;
            }
        });
    }

    private boolean changeRedis(ConcurrentHashMap<String, Long> rate, KeyTime keyTime, String uri, String ip) {
        StringBuilder key = new StringBuilder();
        ip = ip.replaceAll(":", "_");
        ip = ip.replaceAll("\\.", "_");
        key.append(uri).append("-").append(keyTime.getKeyPart()).append("-").append(ip);
        BoundListOperations boundListOperations = redisTemplate.boundListOps(key.toString());
        Long size = boundListOperations.size();

        if (size >= rate.get(uri)) {
            return false;
        } else if (rate.get(uri) > size && size > 0) {
            boundListOperations.rightPushIfPresent(1);
        } else if (size == 0) {
            addKey(key.toString(), keyTime.getLifecycle());
        }
        return true;
    }

    public boolean verify(String hostUri, String ip) {
        if (!ipRateConf.getS().equals(ipRateConfClone.getS())) {
            changeConf(ipRateConf.getS(), sRate);
        }
        if (!ipRateConf.getM().equals(ipRateConfClone.getM())) {
            changeConf(ipRateConf.getM(), mRate);
        }
        if (!ipRateConf.getH().equals(ipRateConfClone.getH())) {
            changeConf(ipRateConf.getH(), hRate);
        }
        if (!ipRateConf.getD().equals(ipRateConfClone.getD())) {
            changeConf(ipRateConf.getD(), dRate);
        }
        if (sRate.get(hostUri) != null) {
            return changeRedis(sRate, KeyTime.S, hostUri, ip);
        } else if (mRate.get(hostUri) != null) {
            return changeRedis(mRate, KeyTime.M, hostUri, ip);
        } else if (hRate.get(hostUri) != null) {
            return changeRedis(hRate, KeyTime.H, hostUri, ip);
        } else if (dRate.get(hostUri) != null) {
            return changeRedis(dRate, KeyTime.D, hostUri, ip);
        } else {
            return true;
        }
    }


    enum KeyTime {
        S("s", 1), M("m", 60), H("h", 60 * 60), D("d", 60 * 60 * 60);
        private String keyPart;
        private int lifecycle;

        KeyTime(String keyPart, int lifecycle) {
            this.keyPart = keyPart;
            this.lifecycle = lifecycle;
        }

        public String getKeyPart() {
            return keyPart;
        }

        public int getLifecycle() {
            return lifecycle;
        }
    }
}
