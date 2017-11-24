> 虽然笔者的初衷是研发一套Java版本的WAF,但是该项目可以作为统一的API Gateway,支持TLS/MITM,还支持下游代理为Socks5,对于需要访问国外被封SaaS服务的http请求来说非常方便.


### 特性
1. 支持"hijacking" HTTPS connection using "Man in the Middle" style attack.
2. 支持HTTP proxy, HTTPS through CONNECT.
3. 支持非法参数拦截.
4. 支持下游代理为Socks5.

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
下游的Proxy,而HostResolver则是WAF自身完成映射.需要特别注意的是,Proxy Chain中如果存在多Proxy是不会负载均衡的,只有前一个不可用时才会用下一个.

**HttpRequestFilterChain** 和 **HttpResponseFilterChain** 责任链,分别对进来和出去的数据进行拦截分析.Request拦截又分为黑白名单两种,Response拦截主要给输出的数据进行安全加固.在Request的拦截规则方面,我参考了[loveshell/ngx_lua_waf](https://github.com/loveshell/ngx_lua_waf).

更多技术详情请移步个人[Java版WAF技术细节](http://www.yangguo.info/2017/06/06/Java%E7%89%88WAF%E5%AE%9E%E7%8E%B0/#more)
[HttpProxy研发心得](http://www.yangguo.info/2017/11/13/HttpProxy%E7%A0%94%E5%8F%91%E5%BF%83%E5%BE%97/#more)

### 性能

##### 测试目的
Nginx的性能是有目共睹的,WAF既然作为一个HTTP Proxy,所以需要跟Nginx对比一下,看看性能的差距有多大.

因为目的是要压出中间Proxy的性能极限,所以后端服务性能要非常高,至少要比中间Proxy性能好,所以选用了Nginx模拟后端服务.
为了减少网络开销对测试影响,所有的测试都是在一台机器上完成的.


##### 测试基准:
1.AB->Nginx_Proxy->Nginx_AS

2.AB->WAF->Nginx_AS

3.ab -k -c 100 -n 1000000 目标地址(HTTP长链)

4.ab -c 100 -n 1000000 目标地址(HTTP短链)


##### JDK版本
```
java version "1.8.0_131"
Java(TM) SE Runtime Environment (build 1.8.0_131-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.131-b11, mixed mode)
```

##### WAF JVM配置:
```
wrapper.java.additional.1=-server
wrapper.java.additional.2=-Xms2048m
wrapper.java.additional.3=-Xmx2048m
wrapper.java.additional.4=-Xmn800m
wrapper.java.additional.5=-XX:+UseG1GC
wrapper.java.additional.6=-Xloggc:/tmp/log/gc.log
wrapper.java.additional.7=-XX:+HeapDumpOnOutOfMemoryError
wrapper.java.additional.8=-XX:+PrintGCDetails
wrapper.java.additional.9=-XX:+PrintGCTimeStamps
wrapper.java.additional.10=-XX:+PreserveFramePointer
```

##### WAF参数配置:
```
#url路径拦截
waf.url=on
#cookie拦截
waf.cookie=on
#user agent拦截
waf.ua=on
#post body参数拦截
waf.post=on
#url参数拦截
waf.args=on
#文件拦截
waf.file=on
#cc拦截
waf.cc=on
#扫描器拦截
waf.scanner=on
#每秒rate
waf.cc.rate=10000
#on表示waf支持loadbalance,需要配置upstream.properties,与waf.proxy.chain和waf.mitm互斥
waf.proxy.lb=off
#设置重试间隔时间，默认10秒
waf.proxy.lb.fail_timeout=10
#是否路由到waf下游的proxy,与waf.proxy.lb互斥
waf.proxy.chain=off
#waf下游的proxy,多个用","分隔.注意只有前一个不可用,才会用下一个,下游proxy不会负载均衡
waf.proxy.chain.servers=127.0.0.1:8180
#是否启用TLS,与waf.mitm互斥
waf.tls=off
#是否HTTPS开启中间人拦截,与waf.tls和waf.proxy.lb互斥
waf.mitm=on
#ip白名单
waf.ip.whitelist=on
#ip黑名单
waf.ip.blacklist=on
#url白名单
waf.url.whitelist=on
#接收者线程数
waf.acceptorThreads=20
#处理client请求的工作线程数
waf.clientToProxyWorkerThreads=100
#处理proxy与后端服务器的工作线程数
waf.proxyToServerWorkerThreads=100
#waf服务器端口
waf.serverPort=8080
```

##### 服务器/虚拟机(测试机)配置:

```
4  Intel(R) Xeon(R) CPU E5-2640 v2 @ 2.00GHz
```


#### 结果:

#### CPU(id基本在10以内)

```
%Cpu0  : 49.8 us, 33.7 sy,  0.0 ni,  6.1 id,  0.0 wa,  0.0 hi, 10.4 si,  0.0 st
%Cpu1  : 48.0 us, 33.9 sy,  0.0 ni,  7.4 id,  0.0 wa,  0.0 hi, 10.7 si,  0.0 st
%Cpu2  : 49.8 us, 33.0 sy,  0.0 ni,  7.4 id,  0.0 wa,  0.0 hi,  9.8 si,  0.0 st
%Cpu3  : 48.8 us, 31.5 sy,  0.0 ni,  8.5 id,  0.0 wa,  0.0 hi, 11.2 si,  0.0 st
```

#### QPS

测试场景|测试条件|QPS
-------|-------|-------
AB->Nginx_AS|HTTP长链|64815
AB->Nginx_AS|HTTP短链|6174
AB->Nginx_Proxy->Nginx_AS|HTTP长链|16924
AB->Nginx_Proxy->Nginx_AS|HTTP短链|13137
AB->WAF->Nginx_AS|HTTP长链|5566
AB->WAF->Nginx_AS|HTTP短链|5559


#### 火焰图:

github不支持火焰图显示,[点击下载源文件](https://github.com/chengdedeng/waf/blob/master/doc/flamegraph.svg).

![](https://github.com/chengdedeng/waf/blob/master/doc/framegraph.png)


#### 常见问题
1. 开启TLS or MITM后,会在项目的目录下生成waf_cert证书,TLS会自动下发证书,MITM需要手动加入证书,信任之后就可以正常工作了.
2. `waf.proxy.lb`和`waf.proxy.mitm`,`waf.tls`和`waf.proxy.mitm`,`waf.proxy.chain`和`waf.proxy.lb`两两之间只能开启其中之一.
3. 如果只是HTTP或者HTTPS抓包,可以关闭所有的安全拦截.
