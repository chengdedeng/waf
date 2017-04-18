package com.chinaredstar.waf.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author:杨果
 * @date:2017/4/18 上午11:17
 *
 * Description:
 *
 * 权重轮询调度算法(WeightedRound-RobinScheduling)-Java实现
 */


public class WeightedRoundRobinScheduling {
    private int currentIndex = -1;// 上一次选择的服务器
    private int currentWeight = 0;// 当前调度的权值
    private int maxWeight = 0; // 最大权重
    private int gcdWeight = 0; //所有服务器权重的最大公约数
    private int serverCount = 0; //服务器数量
    private List<Server> serverList; //服务器集合

    /**
     * 返回最大公约数
     */
    private static int gcd(int a, int b) {
        BigInteger b1 = new BigInteger(String.valueOf(a));
        BigInteger b2 = new BigInteger(String.valueOf(b));
        BigInteger gcd = b1.gcd(b2);
        return gcd.intValue();
    }


    /**
     * 返回所有服务器权重的最大公约数
     */
    private static int getGCDForServers(List<Server> serverList) {
        int w = 0;
        for (int i = 0, len = serverList.size(); i < len - 1; i++) {
            if (w == 0) {
                w = gcd(serverList.get(i).weight, serverList.get(i + 1).weight);
            } else {
                w = gcd(w, serverList.get(i + 1).weight);
            }
        }
        return w;
    }

    /**
     * 返回所有服务器中的最大权重
     */
    public static int getMaxWeightForServers(List<Server> serverList) {
        int w = 0;
        for (int i = 0, len = serverList.size(); i < len - 1; i++) {
            if (w == 0) {
                w = Math.max(serverList.get(i).weight, serverList.get(i + 1).weight);
            } else {
                w = Math.max(w, serverList.get(i + 1).weight);
            }
        }
        return w;
    }

    /**
     * 算法流程： 假设有一组服务器 S = {S0, S1, …, Sn-1} 有相应的权重，变量currentIndex表示上次选择的服务器
     * 权值currentWeight初始化为0，currentIndex初始化为-1 ，当第一次的时候返回 权值取最大的那个服务器， 通过权重的不断递减 寻找
     * 适合的服务器返回，直到轮询结束，权值返回为0
     */
    public Server getServer() {
        while (true) {
            currentIndex = (currentIndex + 1) % serverCount;
            if (currentIndex == 0) {
                currentWeight = currentWeight - gcdWeight;
                if (currentWeight <= 0) {
                    currentWeight = maxWeight;
                    if (currentWeight == 0)
                        return null;
                }
            }
            if (serverList.get(currentIndex).weight >= currentWeight) {
                return serverList.get(currentIndex);
            }
        }
    }

    public static class Server {
        public String ip;
        public int port;
        public int weight;

        public Server(String ip, int port, int weight) {
            super();
            this.ip = ip;
            this.port = port;
            this.weight = weight;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }

    public WeightedRoundRobinScheduling(List<Server> serverList) {
        this.serverList = serverList;
        this.currentIndex = -1;
        this.currentWeight = 0;
        this.serverCount = serverList.size();
        this.maxWeight = getMaxWeightForServers(serverList);
        this.gcdWeight = getGCDForServers(serverList);
    }


    public static void main(String[] args) {
        Server s1 = new Server("192.168.0.100", 80, 1);//3
        Server s2 = new Server("192.168.0.101", 80, 1);//2
        Server s3 = new Server("192.168.0.102", 80, 1);//6
        Server s4 = new Server("192.168.0.103", 80, 1);//4
        Server s5 = new Server("192.168.0.104", 80, 1);//1
        List<Server> serverList = new ArrayList<>();
        serverList.add(s1);
        serverList.add(s2);
        serverList.add(s3);
        serverList.add(s4);
        serverList.add(s5);
        WeightedRoundRobinScheduling obj = new WeightedRoundRobinScheduling(serverList);

        Map<String, Integer> countResult = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            Server s = obj.getServer();
            String log = "ip:" + s.ip + ";weight:" + s.weight;
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
