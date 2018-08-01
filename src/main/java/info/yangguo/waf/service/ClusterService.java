package info.yangguo.waf.service;

import info.yangguo.waf.model.*;

import java.util.Map;
import java.util.Optional;

public interface ClusterService {
    /**
     * 获取所有request filter配置信息
     *
     * @return
     */
    Map<String, SecurityConfig> getRequestConfigs();

    /**
     * 设置request filter开关
     *
     * @param filterName
     * @param config
     */
    void setRequestConfig(Optional<String> filterName, Optional<BasicConfig> config);

    /**
     * 设置request rule开关
     *
     * @param filterName
     * @param iterm
     * @param config
     */
    void setRequestItermConfig(Optional<String> filterName, Optional<String> iterm, Optional<BasicConfig> config);

    /**
     * 删除request iterm
     *
     * @param filterName
     * @param iterm
     */
    void deleteRequestIterm(Optional<String> filterName, Optional<String> iterm);

    /**
     * 获取response filter配置信息
     *
     * @return
     */
    Map<String, ResponseConfig> getResponseConfigs();

    /**
     * 设置response filter开关
     *
     * @param filterName
     * @param config
     */
    void setResponseConfig(Optional<String> filterName, Optional<BasicConfig> config);

    /**
     * 获取upstream server配置
     *
     * @return
     */
    Map<String, WeightedRoundRobinScheduling> getUpstreamConfig();

    /**
     * 设置host配置
     *
     * @param hostOptional
     * @param config
     */
    void setUpstreamConfig(Optional<String> hostOptional, Optional<BasicConfig> config);

    /**
     * 设置指定host中server的配置
     *
     * @param hostOptional
     * @param ipOptional
     * @param portOptional
     * @param config
     */
    void setUpstreamServerConfig(Optional<String> hostOptional, Optional<String> ipOptional, Optional<Integer> portOptional, Optional<ServerBasicConfig> config);

    /**
     * 删除host
     *
     * @param hostOptional
     */
    void deleteUpstream(Optional<String> hostOptional);

    /**
     * 删除指定host的server
     *
     * @param hostOptional
     * @param ipOptional
     * @param portOptional
     */
    void deleteUpstreamServer(Optional<String> hostOptional, Optional<String> ipOptional, Optional<Integer> portOptional);
}
