package info.yangguo.waf;


import info.yangguo.waf.util.SelfSignedSslEngineSource2;

import org.littleshoot.proxy.ActivityTrackerAdapter;
import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyAdapter;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.FlowContext;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Queue;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

public class Application {
    private static Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws UnknownHostException {
        ThreadPoolConfiguration threadPoolConfiguration = new ThreadPoolConfiguration();
        threadPoolConfiguration.withAcceptorThreads(Constant.AcceptorThreads);
        threadPoolConfiguration.withClientToProxyWorkerThreads(Constant.ClientToProxyWorkerThreads);
        threadPoolConfiguration.withProxyToServerWorkerThreads(Constant.ProxyToServerWorkerThreads);

        InetSocketAddress inetSocketAddress = new InetSocketAddress(Constant.ServerPort);
        HttpProxyServerBootstrap httpProxyServerBootstrap = DefaultHttpProxyServer.bootstrap()
                .withAddress(inetSocketAddress);
        if (!"on".equals(Constant.wafConfs.get("waf.reverse.proxy"))) {
            //透明代理模式
            logger.debug("透明代理模式开启");
            String reverseProxy = Constant.wafConfs.get("waf.chain.proxy.servers");
            final String[] reProxys = reverseProxy.split(",");
            httpProxyServerBootstrap.withChainProxyManager(new ChainedProxyManager() {
                @Override
                public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies) {
                    for (final String reProxy : reProxys) {
                        chainedProxies.add(new ChainedProxyAdapter() {
                            @Override
                            public InetSocketAddress getChainedProxyAddress() {
                                String[] rpInfo = reProxy.split(":");
                                return new InetSocketAddress(rpInfo[0], Integer.parseInt(rpInfo[1]));
                            }
                        });
                    }
                }
            });
        } else {
            //反向代理模式
            logger.debug("反向代理模式开启");
            httpProxyServerBootstrap.withServerResolver(HostResolverImpl.getSingleton());
        }
        if ("on".equals(Constant.wafConfs.get("waf.tls"))) {
            httpProxyServerBootstrap
                    //不需要验证client端证书
                    .withAuthenticateSslClients(false)
                    .withSslEngineSource(new SelfSignedSslEngineSource2("waf.jks", true, true));
        }
        httpProxyServerBootstrap.withAllowRequestToOriginServer(true)
                .withProxyAlias("waf")
                .withThreadPoolConfiguration(threadPoolConfiguration)
                .plusActivityTracker(new ActivityTrackerAdapter() {
                    @Override
                    public void requestReceivedFromClient(FlowContext flowContext,
                                                          HttpRequest httpRequest) {
                        InetSocketAddress clientAddress = flowContext.getClientAddress();
                        //将请求源地址塞入header带给过滤器
                        httpRequest.headers().add("X-Proxy-IP", clientAddress.getAddress().getHostAddress());
                    }
                })
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new HttpFilterAdapterImpl(originalRequest, ctx);
                    }
                })
                .start();
    }
}
