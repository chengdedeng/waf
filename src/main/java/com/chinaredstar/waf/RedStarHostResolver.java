package com.chinaredstar.waf;

import com.chinaredstar.waf.util.PropertiesUtil;
import com.chinaredstar.waf.util.WeightedRoundRobinScheduling;

import org.littleshoot.proxy.HostResolver;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author:杨果
 * @date:2017/3/31 下午7:28
 *
 * Description:
 *
 */
public class RedStarHostResolver implements HostResolver {
    private static Map<String, WeightedRoundRobinScheduling> serverMap = new HashMap<>();
    private final static RedStarHostResolver redStarHostResolver = new RedStarHostResolver();

    private RedStarHostResolver() {
        Map<String, String> servers = PropertiesUtil.getProperty("upstream.properties");
        for (Map.Entry<String, String> entry : servers.entrySet()) {
            String hostInfo = entry.getKey();
            String[] serversInfo = entry.getValue().split(",");
            List<WeightedRoundRobinScheduling.Server> serverList = new ArrayList<>();
            for (String serverInfo : serversInfo) {
                String[] si = serverInfo.split(":");
                if (si.length == 2) {
                    WeightedRoundRobinScheduling.Server server = new WeightedRoundRobinScheduling.Server(si[0], Integer.valueOf(si[1]), 1);
                    serverList.add(server);
                } else if (si.length == 3) {
                    WeightedRoundRobinScheduling.Server server = new WeightedRoundRobinScheduling.Server(si[0], Integer.valueOf(si[1]), Integer.valueOf(si[2]));
                    serverList.add(server);
                }
            }

            serverMap.put(hostInfo, new WeightedRoundRobinScheduling(serverList));
        }
    }

    public static RedStarHostResolver getInstance() {
        return redStarHostResolver;
    }


    @Override
    public InetSocketAddress resolve(String host, int port)
            throws UnknownHostException {
        String key = host + "_" + port;
        if (serverMap.containsKey(key)) {
            WeightedRoundRobinScheduling.Server server = serverMap.get(key).getServer();
            return new InetSocketAddress(server.getIp(), server.getPort());
        } else {
            return null;
        }
    }
}
