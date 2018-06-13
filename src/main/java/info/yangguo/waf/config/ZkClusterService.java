package info.yangguo.waf.config;

import info.yangguo.waf.model.Config;
import info.yangguo.waf.model.RequestConfig;

import java.time.Duration;
import java.util.Map;

public class ZkClusterService implements ClusterService {
    @Override
    public String getSession(String sessionId) {
        return null;
    }

    @Override
    public void setSession(String sessionId, String sessionValue, Duration ttl) {

    }

    @Override
    public void deleteSession(String sessionId) {

    }

    @Override
    public Map<String, RequestConfig> getRequestConfigs() {
        return null;
    }

    @Override
    public void setRequestSwitch(String filterName, Boolean isStart) {

    }

    @Override
    public void setRequestRule(String filterName, String rule, Boolean isStart) {

    }

    @Override
    public void deleteRequestRule(String filterName, String rule) {

    }

    @Override
    public Map<String, Config> getResponseConfigs() {
        return null;
    }

    @Override
    public void setResponseSwitch(String filterName, Boolean isStart) {

    }
}
