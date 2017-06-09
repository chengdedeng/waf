package info.yangguo.waf;

import info.yangguo.waf.util.PropertiesUtil;
import info.yangguo.waf.util.WeightedRoundRobinScheduling;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.littleshoot.proxy.HostResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author:杨果
 * @date:2017/3/31 下午7:28
 *
 * Description:
 *
 */
class HostResolverImpl implements HostResolver {
    private static Logger logger = LoggerFactory.getLogger(HostResolverImpl.class);
    private volatile static HostResolverImpl singleton;
    private Map<String, WeightedRoundRobinScheduling> serverMap = new HashMap<>();
    private final HttpClient client = HttpClientBuilder.create().build();

    private class ServerCheckTask implements Runnable {
        @Override
        public void run() {
            try {
                for (Map.Entry<String, WeightedRoundRobinScheduling> entry : serverMap.entrySet()) {
                    WeightedRoundRobinScheduling weightedRoundRobinScheduling = entry.getValue();
                    List<WeightedRoundRobinScheduling.Server> delServers = new ArrayList<>();
                    CloseableHttpResponse httpResponse = null;
                    for (WeightedRoundRobinScheduling.Server server : weightedRoundRobinScheduling.unhealthilyServers) {
                        HttpGet request = new HttpGet("http://" + server.getIp() + ":" + server.getPort());
                        try {
                            httpResponse = (CloseableHttpResponse) client.execute(request);
                            weightedRoundRobinScheduling.healthilyServers.add(weightedRoundRobinScheduling.serversMap.get(server.getIp() + "_" + server.getPort()));
                            delServers.add(server);
                            logger.info("domain host->{},ip->{},port->{} is healthy", entry.getKey(), server.getIp(), server.getPort());
                        } catch (ConnectException e1) {
                            logger.warn("domain host->{},ip->{},port->{} is unhealthy", entry.getKey(), server.getIp(), server.getPort());
                        } catch (Exception e2) {
                            weightedRoundRobinScheduling.healthilyServers.add(weightedRoundRobinScheduling.serversMap.get(server.getIp() + "_" + server.getPort()));
                            delServers.add(server);
                            logger.info("domain host->{},ip->{},port->{} is healthy", server.getIp(), server.getPort());
                        } finally {
                            if (httpResponse != null) {
                                httpResponse.close();
                            }
                        }
                    }
                    if (delServers.size() > 0) {
                        weightedRoundRobinScheduling.unhealthilyServers.removeAll(delServers);
                    }
                }
            } catch (Exception e) {
                logger.error("server check task->{}", e);
            }
        }
    }

    private HostResolverImpl() {
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
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new ServerCheckTask(), Integer.parseInt(Constant.wafConfs.get("waf.proxy.lb.fail_timeout")), Integer.parseInt(Constant.wafConfs.get("waf.proxy.lb.fail_timeout")), TimeUnit.SECONDS);
    }

    public static HostResolverImpl getSingleton() {
        if (singleton == null) {
            synchronized (HostResolverImpl.class) {
                if (singleton == null) {
                    singleton = new HostResolverImpl();
                }
            }
        }
        return singleton;
    }


    WeightedRoundRobinScheduling getServers(String key) {
        return serverMap.get(key);
    }

    @Override
    public InetSocketAddress resolve(String host, int port)
            throws UnknownHostException {
        String key = host + "_" + port;
        if (serverMap.containsKey(key)) {
            WeightedRoundRobinScheduling.Server server = serverMap.get(key).getServer();
            if (server != null) {
                return new InetSocketAddress(server.getIp(), server.getPort());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
