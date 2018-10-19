/*
 * Copyright 2018-present yangguo@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.yangguo.waf.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author:杨果
 * @date:2017/4/18 上午11:17
 * <p>
 * Description:
 * <p>
 * 权重轮询调度算法(WeightedRound-RobinScheduling)-Java实现
 */
@NoArgsConstructor
public class WeightedRoundRobinScheduling implements Serializable {
    private static final long serialVersionUID = -3030570670940352661L;
    @Getter
    @Setter
    private BasicConfig basicConfig;//host对应的upstream是否开启
    private int currentIndex = -1;// 上一次选择的服务器
    private int currentWeight = 0;// 当前调度的权值
    @Getter
    private CopyOnWriteArrayList<ServerConfig> healthilyServerConfigs = new CopyOnWriteArrayList(); //健康服务器集合
    @Getter
    private CopyOnWriteArrayList<ServerConfig> unhealthilyServerConfigs = new CopyOnWriteArrayList<>(); //不健康服务器集合
    @Getter
    private Map<String, ServerConfig> serversMap = new HashMap<>();

    /**
     * 返回最大公约数
     */
    private int gcd(int a, int b) {
        BigInteger b1 = new BigInteger(String.valueOf(a));
        BigInteger b2 = new BigInteger(String.valueOf(b));
        BigInteger gcd = b1.gcd(b2);
        return gcd.intValue();
    }


    /**
     * 返回所有服务器权重的最大公约数
     */
    private int getGCDForServers(List<ServerConfig> serverConfigList) {
        int w = 0;
        for (int i = 0, len = serverConfigList.size(); i < len - 1; i++) {
            if (w == 0) {
                w = gcd(serverConfigList.get(i).config.getWeight(), serverConfigList.get(i + 1).getConfig().getWeight());
            } else {
                w = gcd(w, serverConfigList.get(i + 1).getConfig().getWeight());
            }
        }
        return w;
    }

    /**
     * 返回所有服务器中的最大权重
     */
    private int getMaxWeightForServers(List<ServerConfig> serverConfigList) {
        int w = 0;
        for (int i = 0, len = serverConfigList.size(); i < len - 1; i++) {
            if (w == 0) {
                w = Math.max(serverConfigList.get(i).getConfig().getWeight(), serverConfigList.get(i + 1).getConfig().getWeight());
            } else {
                w = Math.max(w, serverConfigList.get(i + 1).getConfig().getWeight());
            }
        }
        return w;
    }

    /**
     * 算法流程： 假设有一组服务器 S = {S0, S1, …, Sn-1} 有相应的权重，变量currentIndex表示上次选择的服务器
     * 权值currentWeight初始化为0，currentIndex初始化为-1 ，当第一次的时候返回 权值取最大的那个服务器， 通过权重的不断递减 寻找
     * 适合的服务器返回，直到轮询结束，权值返回为0
     */
    @JsonIgnore
    public ServerConfig getServer() {
        if (healthilyServerConfigs.size() == 0) {
            return null;
        } else if (healthilyServerConfigs.size() == 1) {
            return healthilyServerConfigs.get(0);
        } else {
            while (true) {
                currentIndex = (currentIndex + 1) % healthilyServerConfigs.size();
                if (currentIndex == 0) {
                    currentWeight = currentWeight - getGCDForServers(healthilyServerConfigs);
                    if (currentWeight <= 0) {
                        currentWeight = getMaxWeightForServers(healthilyServerConfigs);
                        if (currentWeight == 0)
                            return null;
                    }
                }
                if (healthilyServerConfigs.get(currentIndex).getConfig().getWeight() >= currentWeight) {
                    return healthilyServerConfigs.get(currentIndex);
                }
            }
        }
    }

    public WeightedRoundRobinScheduling(List<ServerConfig> serverConfigs, BasicConfig basicConfig) {
        this.basicConfig = basicConfig;
        if (basicConfig != null && basicConfig.getIsStart()) {
            serverConfigs.stream().forEach(server -> {
                if (server.getConfig().getIsStart())
                    healthilyServerConfigs.add(server);
            });
        }
        serverConfigs.stream().forEach(server -> {
            serversMap.put(server.getIp() + "_" + server.getPort(), server);
        });
    }
}
