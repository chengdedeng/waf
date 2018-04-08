package info.yangguo.waf;


import info.yangguo.waf.util.NetUtils;
import info.yangguo.waf.util.WafSelfSignedSslEngineSource;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.KeyStoreFileCertificateSource;
import net.lightbody.bmp.mitm.keys.ECKeyGenerator;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
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
        httpProxyServerBootstrap.withIdleConnectionTimeout(Constant.IdleConnectionTimeout);
        boolean proxy_chain = "on".equals(Constant.wafConfs.get("waf.chain"));
        boolean proxy_lb = "on".equals(Constant.wafConfs.get("waf.lb"));
        boolean proxy_tls = "on".equals(Constant.wafConfs.get("waf.tls"));
        boolean proxy_mitm = "on".equals(Constant.wafConfs.get("waf.mitm"));
        boolean proxy_ss = "on".equals(Constant.wafConfs.get("waf.ss"));
        if (proxy_chain && proxy_lb) {
            logger.error("waf.chain和waf.lb只能开启其中之一");
            throw new IllegalArgumentException("waf.chain和waf.lb只能开启其中之一");
        }
        if (proxy_tls && proxy_mitm) {
            logger.error("waf.tls和waf.mitm只能开启其中之一");
            throw new IllegalArgumentException("waf.tls和waf.mitm只能开启其中之一");
        }
        if(proxy_lb && proxy_mitm){
            logger.error("waf.lb和waf.mitm只能开启其中之一");
            throw new IllegalArgumentException("waf.lb和waf.mitm只能开启其中之一");
        }
        if(proxy_lb && proxy_ss){
            logger.error("waf.lb和waf.ss只能开启其中之一");
            throw new IllegalArgumentException("waf.lb和waf.mitm只能开启其中之一");
        }
        if(proxy_mitm && proxy_ss){
            logger.error("waf.mitm和waf.ss只能开启其中之一");
            throw new IllegalArgumentException("waf.lb和waf.mitm只能开启其中之一");
        }
        if(proxy_chain || proxy_ss){
            logger.info("透明代理模式开启");
        }
        if (proxy_chain) {
            //透明代理模式
            String reverseProxy = Constant.wafConfs.get("waf.chain.servers");
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
        } else if (proxy_lb) {
            //反向代理模式
            logger.info("反向代理模式开启");
            httpProxyServerBootstrap.withServerResolver(HostResolverImpl.getSingleton());
        }
        if (proxy_tls) {
            logger.info("开启TLS支持");
            httpProxyServerBootstrap
                    //不验证client端证书
                    .withAuthenticateSslClients(false)
                    .withSslEngineSource(new WafSelfSignedSslEngineSource());
        } else if (proxy_mitm) {
            logger.info("开启中间人拦截支持支持");
            WafSelfSignedSslEngineSource wafSelfSignedSslEngineSource = new WafSelfSignedSslEngineSource();
            CertificateAndKeySource certificateAndKeySource;
            certificateAndKeySource = new KeyStoreFileCertificateSource(
                    "JKS",
                    wafSelfSignedSslEngineSource.keyStoreFile,
                    "waf",
                    WafSelfSignedSslEngineSource.PASSWORD);


            ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                    .rootCertificateSource(certificateAndKeySource)
                    .serverKeyGenerator(new ECKeyGenerator())
                    .build();
            httpProxyServerBootstrap.withManInTheMiddle(mitmManager);
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
