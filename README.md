> 网上有很多基于Nginx和Apache的软件WAF开源方案,笔者为了学习所以用Java重造了一个轮子.

### Quick Start

##### 编译:
```
 mvn package 
```

##### 运行:
由于使用[appassembler-maven-plugin](http://www.mojohaus.org/appassembler/appassembler-maven-plugin/usage-jsw.html)
打成了符合[JSW](https://wrapper.tanukisoftware.com/doc/english/download.jsp)规范的包,所以解压target目录下的Zip文件,
然后在bin目录下运行对应平台的脚本,以Linux为例:

```
bin/waf { console | start | stop | restart | status | dump }
```

##### 配置:
配置文件在conf目录下,upstream.properties中配置的是需要反向代理的目标机,waf.properties中配置的是WAF拦截的信息及一些常规配置,wrapper.conf
中是JSW的配置文件,这里面包含JVM配置等信息.


### 架构
HTTP Proxy选择了基于[Netty](https://netty.io/)研发的[LittleProxy](https://github.com/adamfisk/LittleProxy),
LittleProxy是[LANTERN](https://getlantern.org/)的维护者发起的开源项目,是一款非常优秀的Java HTTP Proxy.
关于Loadbalance,WAF有两种模式可以供选择,一种基于Proxy Chain,例一种是基于HostResolver.Proxy Chain是把目标机的映射交给
下游的Proxy,而HostResolver则是WAF自身完成映射.需要特别注意的是,Proxy Chain中如果存在多Proxy是不会负载均衡的,只有前一个
不可用才会用下一个.

**HttpRequestFilterChain** 和 **HttpResponseFilterChain** 责任链,分别对进来和出去的数据尽心拦截分析.Request拦截
又分为黑白名单两种,Response拦截主要给输出的数据进行安全加固.在Request的拦截规则方面,我参考了[loveshell/ngx_lua_waf](https://github.com/loveshell/ngx_lua_waf).

### 性能

##### 测试目的
Nginx的性能是有目共睹的,WAF既然作为一个HTTP Proxy,所以需要跟Nginx对比一下,看看性能的差距有多大.

因为目的是要压出中间Proxy的性能极限,所以后端服务性能要非常高,至少要比中间Proxy性能好,所以选用了Nginx模拟后端服务.
为了减少网络开销对测试影响,所有的测试都是在一台机器上完成的.


##### 测试基准:
1.AB->Nginx_Proxy->Nginx_AS 
2.AB->WAF->Nginx_AS
3.ab -k -c 300 -n 1000000 目标地址(HTTP长链)
4.ab -c 300 -n 1000000 目标地址(HTTP短链)

##### WAF JVM配置:
```
wrapper.java.additional.1=-server
wrapper.java.additional.2=-Xms2048m
wrapper.java.additional.3=-Xmx2048m
wrapper.java.additional.4=-Xmn500m
wrapper.java.additional.5=-XX:PermSize=128m
wrapper.java.additional.6=-XX:MaxPermSize=128m
wrapper.java.additional.7=-XX:TargetSurvivorRatio=80
wrapper.java.additional.8=-XX:+UseConcMarkSweepGC
wrapper.java.additional.9=-XX:+CMSClassUnloadingEnabled
wrapper.java.additional.10=-Xloggc:/tmp/log/gc.log
wrapper.java.additional.11=-XX:+HeapDumpOnOutOfMemoryError
wrapper.java.additional.12=-XX:+PrintGCDetails
wrapper.java.additional.13=-XX:+PrintGCTimeStamps
```

##### 服务器(测试机)配置:

```
4  Intel(R) Xeon(R) CPU E5-2640 v2 @ 2.00GHz
```

#### 结果:

测试场景|测试条件|QPS
-------|-------|-------
AB->Nginx_AS|HTTP长链|70312
AB->Nginx_AS|HTTP短链|5317
AB->Nginx_Proxy->Nginx_AS|HTTP长链|15347
AB->Nginx_Proxy->Nginx_AS|HTTP短链|14108
AB->WAF->Nginx_AS|HTTP长链|1588
AB->WAF->Nginx_AS|HTTP短链|1545