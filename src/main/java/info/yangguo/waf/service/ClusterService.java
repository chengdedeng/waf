package info.yangguo.waf.service;

import info.yangguo.waf.model.Config;
import info.yangguo.waf.model.RequestConfig;
import info.yangguo.waf.model.WeightedRoundRobinScheduling;

import java.util.Map;
import java.util.Optional;

public interface ClusterService {
    /**
     * 获取所有request filter配置信息
     *
     * @return
     */
    Map<String, RequestConfig> getRequestConfigs();

    /**
     * 设置request filter开关
     *
     * @param filterName
     * @param isStart
     */
    void setRequestSwitch(String filterName, Boolean isStart);

    /**
     * 设置request rule开关
     *
     * @param filterName
     * @param rule
     * @param isStart
     */
    void setRequestRule(String filterName, String rule, Boolean isStart);

    /**
     * 删除request rule
     *
     * @param filterName
     * @param rule
     */
    void deleteRequestRule(String filterName, String rule);

    /**
     * 获取response filter配置信息
     *
     * @return
     */
    Map<String, Config> getResponseConfigs();

    /**
     * 设置response filter开关
     *
     * @param filterName
     * @param isStart
     */
    void setResponseSwitch(String filterName, Boolean isStart);

    /**
     * 获取upstream server配置
     *
     * @return
     */
    Map<String, WeightedRoundRobinScheduling> getUpstreamConfig();

    /**
     * 设置host开关
     *
     * @param hostOptional
     * @param isStartOptional
     */
    void setUpstream(Optional<String> hostOptional, Optional<Boolean> isStartOptional);

    /**
     * 设置指定host中server的开关
     *
     * @param hostOptional
     * @param ipOptional
     * @param portOptional
     * @param isStartOptional
     */
    void setUpstreamServer(Optional<String> hostOptional, Optional<String> ipOptional, Optional<Integer> portOptional, Optional<Boolean> isStartOptional, Optional<Integer> weightOptional);

    /**
     * 删除host
     *
     * @param hostOptional
     */
    void deleteUpstream(Optional<String> hostOptional);

    /**
     * 删除host的server
     *
     * @param hostOptional
     * @param ipOptional
     * @param portOptional
     */
    void deleteUpstreamServer(Optional<String> hostOptional, Optional<String> ipOptional, Optional<Integer> portOptional);
}
