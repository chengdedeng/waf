package info.yangguo.waf;


import info.yangguo.waf.util.NetUtils;
import info.yangguo.waf.util.WafSelfSignedSslEngineSource;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.*;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Queue;

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
        if (!"on".equals(Constant.wafConfs.get("waf.proxy.lb"))) {
            //透明代理模式
            logger.debug("透明代理模式开启");
            String reverseProxy = Constant.wafConfs.get("waf.proxy.chain.servers");
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
                    //不验证client端证书
                    .withAuthenticateSslClients(false)
                    .withSslEngineSource(new WafSelfSignedSslEngineSource());
        }
        httpProxyServerBootstrap.withAllowRequestToOriginServer(true)
                .withProxyAlias("waf")
                .withThreadPoolConfiguration(threadPoolConfiguration)
                //XFF设置
                .plusActivityTracker(new ActivityTrackerAdapter() {
                    @Override
                    public void requestReceivedFromClient(FlowContext flowContext,
                                                          HttpRequest httpRequest) {

                        String xffKey = "X-Forwarded-For";
                        StringBuilder xff = new StringBuilder();
                        List<String> headerValues1 = Constant.getHeaderValues(httpRequest, xffKey);
                        if (headerValues1.size() > 0 && headerValues1.get(0) != null) {
                            //逗号面一定要带一个空格
                            xff.append(headerValues1.get(0)).append(", ");
                        }
                        xff.append(NetUtils.getLocalHost());
                        httpRequest.headers().set(xffKey, xff.toString());
                    }
                })
                //X-Real-IP设置
                .plusActivityTracker(
                        new ActivityTrackerAdapter() {
                            @Override
                            public void requestReceivedFromClient(FlowContext flowContext,
                                                                  HttpRequest httpRequest) {
                                List<String> headerValues2 = Constant.getHeaderValues(httpRequest, "X-Real-IP");
                                if (headerValues2.size() == 0) {
                                    String remoteAddress = flowContext.getClientAddress().getAddress().getHostAddress();
                                    httpRequest.headers().add("X-Real-IP", remoteAddress);
                                }
                            }
                        }
                )
                .withFiltersSource(new HttpFiltersSourceAdapter() {
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new HttpFilterAdapterImpl(originalRequest, ctx);
                    }
                })
                .start();
    }
}
