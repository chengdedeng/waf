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
     * 设置security iterm
     *
     * @param filterName
     * @param iterm
     * @param config
     */
    void setSecurityConfigIterm(Optional<String> filterName, Optional<String> iterm, Optional<BasicConfig> config);

    /**
     * 删除security iterm
     *
     * @param filterName
     * @param iterm
     */
    void deleteSecurityConfigIterm(Optional<String> filterName, Optional<String> iterm);

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
    Map<String, ForwardConfig> getForwardConfigs();

    /**
     * 设置forward config
     *
     * @param wafRoute
     * @param config
     */
    void setForwardConfig(Optional<String> wafRoute, Optional<BasicConfig> config);

    /**
     * 设置forward config iterm
     *
     * @param wafRoute
     * @param iterm
     * @param config
     */
    void setForwardConfigIterm(Optional<String> wafRoute, Optional<String> iterm, Optional<BasicConfig> config);

    /**
     * 删除forward config iterm
     *
     * @param wafRoute
     * @param iterm
     */
    void deleteForwardConfigIterm(Optional<String> wafRoute, Optional<String> iterm);

    /**
     * 删除forward config
     *
     * @param wafRoute
     */
    void deleteForwardConfig(Optional<String> wafRoute);
}
