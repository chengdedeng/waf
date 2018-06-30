package info.yangguo.waf.service;

import info.yangguo.waf.model.Config;
import info.yangguo.waf.model.RequestConfig;

import java.util.Map;

public interface ClusterService {
    /**
     * 获取所有request filter配置信息
     *
     * @return
     */
    Map<String, RequestConfig> getRequestConfigs();

    /**
     * 获取指定request filter的配置信息
     *
     * @param requestFileterClass
     * @return
     */
    RequestConfig getRequestConfig(Class requestFileterClass);

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
     * 获取指定response filter的配置信息
     *
     * @param responseFilterClass
     * @return
     */
    Config getResponseConfig(Class responseFilterClass);

    /**
     * 设置response filter开关
     *
     * @param filterName
     * @param isStart
     */
    void setResponseSwitch(String filterName, Boolean isStart);
}
