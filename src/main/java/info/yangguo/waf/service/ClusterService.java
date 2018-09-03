package info.yangguo.waf.service;

import info.yangguo.waf.model.*;

import java.util.Map;
import java.util.Optional;

public interface ClusterService {
    /**
     * 获取security配置信息
     *
     * @return
     */
    Map<String, SecurityConfig> getSecurityConfigs();

    /**
     * 设置security filter开关
     *
     * @param filterName
     * @param config
     */
    void setSecurityConfig(Optional<String> filterName, Optional<BasicConfig> config);

    /**
     * 设置security item
     *
     * @param filterName
     * @param item
     * @param config
     */
    void setSecurityConfigItem(Optional<String> filterName, Optional<String> item, Optional<BasicConfig> config);

    /**
     * 删除security item
     *
     * @param filterName
     * @param item
     */
    void deleteSecurityConfigItem(Optional<String> filterName, Optional<String> item);

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

    /**
     * 获取rewrite配置信息
     *
     * @return
     */
    Map<String, BasicConfig> getRewriteConfigs();

    /**
     * 设置rewrite配置
     *
     * @param wafRoute
     * @param config
     */
    void setRewriteConfig(Optional<String> wafRoute, Optional<BasicConfig> config);

    /**
     * 删除rewrite配置
     *
     * @param wafRoute
     */
    void deleteRewrite(Optional<String> wafRoute);

    /**
     * 获取redirect配置信息
     *
     * @return
     */
    Map<String, BasicConfig> getRedirectConfigs();

    /**
     * 设置redirect配置
     *
     * @param wafRoute
     * @param config
     */
    void setRedirectConfig(Optional<String> wafRoute, Optional<BasicConfig> config);

    /**
     * 删除redirect配置
     *
     * @param wafRoute
     */
    void deleteRedirect(Optional<String> wafRoute);

    /**
     * 获取forward配置信息
     *
     * @return
     */
    Map<String, ForwardConfig> getTranslateConfigs();

    /**
     * 设置forward config
     *
     * @param wafRoute
     * @param config
     */
    void setTranslateConfig(Optional<String> wafRoute, Optional<BasicConfig> config);

    /**
     * 设置forward config item
     *
     * @param wafRoute
     * @param item
     * @param config
     */
    void setTranslateConfigItem(Optional<String> wafRoute, Optional<String> item, Optional<BasicConfig> config);

    /**
     * 删除forward config item
     *
     * @param wafRoute
     * @param item
     */
    void deleteTranslateConfigItem(Optional<String> wafRoute, Optional<String> item);

    /**
     * 删除forward config
     *
     * @param wafRoute
     */
    void deleteTranslateConfig(Optional<String> wafRoute);
}
