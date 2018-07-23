package info.yangguo.waf;

import info.yangguo.waf.model.BasicConfig;
import info.yangguo.waf.model.ServerConfig;
import info.yangguo.waf.model.ServerBasicConfig;
import info.yangguo.waf.model.WeightedRoundRobinScheduling;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author:杨果
 * @date:2017/5/17 下午2:16
 * <p>
 * Description:
 */
public class WeightedRoundRobinSchedulingTest {
    @Test
    public void test1() {
        ServerBasicConfig serverBasicConfig1 =ServerBasicConfig.builder().weight(4).isStart(true).build();
        ServerBasicConfig serverBasicConfig2 =ServerBasicConfig.builder().weight(2).isStart(true).build();

        ServerConfig s1 = ServerConfig.builder().ip("192.168.0.100").port(81).config(serverBasicConfig1).build();
        ServerConfig s2 = ServerConfig.builder().ip("192.168.0.101").port(82).config(serverBasicConfig1).build();
        ServerConfig s3 = ServerConfig.builder().ip("192.168.0.102").port(83).config(serverBasicConfig1).build();
        ServerConfig s4 = ServerConfig.builder().ip("192.168.0.103").port(84).config(serverBasicConfig2).build();
        ServerConfig s5 = ServerConfig.builder().ip("192.168.0.104").port(85).config(serverBasicConfig2).build();
        List<ServerConfig> serverConfigList = new ArrayList<>();
        serverConfigList.add(s1);
        serverConfigList.add(s2);
        serverConfigList.add(s3);
        serverConfigList.add(s4);
        serverConfigList.add(s5);
        BasicConfig basicConfig =new BasicConfig();
        basicConfig.setIsStart(true);
        WeightedRoundRobinScheduling obj = new WeightedRoundRobinScheduling(serverConfigList, basicConfig);

        Map<String, Integer> countResult = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            ServerConfig s = obj.getServer();
            String log = "ip:" + s.getIp() + ";weight:" + s.getConfig().getWeight();
            if (countResult.containsKey(log)) {
                countResult.put(log, countResult.get(log) + 1);
            } else {
                countResult.put(log, 1);
            }
            System.out.println(log);
        }

        for (Map.Entry<String, Integer> map : countResult.entrySet()) {
            System.out.println("服务器 " + map.getKey() + " 请求次数： " + map.getValue());
        }
    }
}
