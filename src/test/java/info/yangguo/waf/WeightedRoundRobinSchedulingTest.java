package info.yangguo.waf;

import info.yangguo.waf.model.Server;
import info.yangguo.waf.model.ServerConfig;
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
        ServerConfig serverConfig=new ServerConfig();
        serverConfig.setWeight(1);
        serverConfig.setIsStart(true);
        Server s1 = Server.builder().ip("192.168.0.100").port(80).serverConfig(serverConfig).build();//3
        Server s2 = Server.builder().ip("192.168.0.101").port(80).serverConfig(serverConfig).build();//2
        Server s3 = Server.builder().ip("192.168.0.102").port(80).serverConfig(serverConfig).build();//2
        Server s4 = Server.builder().ip("192.168.0.103").port(80).serverConfig(serverConfig).build();//2
        Server s5 = Server.builder().ip("192.168.0.104").port(80).serverConfig(serverConfig).build();//2
        List<Server> serverList = new ArrayList<>();
        serverList.add(s1);
        serverList.add(s2);
        serverList.add(s3);
        serverList.add(s4);
        serverList.add(s5);
        WeightedRoundRobinScheduling obj = new WeightedRoundRobinScheduling(serverList, true);

        Map<String, Integer> countResult = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            Server s = obj.getServer();
            String log = "ip:" + s.getIp() + ";weight:" + s.getServerConfig().getWeight();
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
