package info.yangguo.waf.service;

import info.yangguo.waf.model.Config;
import info.yangguo.waf.model.RequestConfig;

import java.util.Map;

public interface ClusterService {
    /**
     * 获取request filter配置信息
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
}
