> 网上有很多基于Nginx和Apache的软件WAF开源方案,笔者为了学习所以用Java重造了一个轮子.

### Quick Start

`编译:`

```
 mvn package 
```

`运行:`

由于使用[appassembler-maven-plugin](http://www.mojohaus.org/appassembler/appassembler-maven-plugin/usage-jsw.html)
打成了符合[JSW](https://wrapper.tanukisoftware.com/doc/english/download.jsp)规范的包,所以解压target目录下的Zip文件,
然后在bin目录下运行对应平台的脚本,以Linux为例:
```
bin/waf { console | start | stop | restart | status | dump }
```

`配置:`

配置文件在conf目录下,upstream.properties中配置的是需要反向代理的目标机,waf.properties中配置的是WAF拦截的信息及一些常规配置,wrapper.conf
中是JSW的配置文件,这里面包含JVM配置等信息.


支持在线模式中的透明代理模式和反向代理模式 透明代理模式:WAF->loadbalance->应用服务器 反向代理模式:WAF->应用服务器

反向代理模式难点在负载均衡算法实现和节点健康检查,本项目中只实现了简单的round robin,推荐大家使用透明代理模式.