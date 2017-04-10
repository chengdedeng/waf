package com.chinaredstar.waf;


import com.chinaredstar.waf.config.IpRateConf;

import org.littleshoot.proxy.ActivityTrackerAdapter;
import org.littleshoot.proxy.FlowContext;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class Application {
    private static Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws UnknownHostException {
        ApplicationContext factory = new ClassPathXmlApplicationContext("classpath:spring/applicationContext-*.xml");
        final IpRateConf ipRateConf = (IpRateConf) factory.getBean("ipRateConf");
        final RedisTemplate cacheRedisTemplate = (RedisTemplate) factory.getBean("cacheRedisTemplate");


        final IpRateUtil ipRateUtil = new IpRateUtil(ipRateConf, cacheRedisTemplate);

        ThreadPoolConfiguration threadPoolConfiguration = new ThreadPoolConfiguration();
        threadPoolConfiguration.withAcceptorThreads(5);
        threadPoolConfiguration.withClientToProxyWorkerThreads(20);
        threadPoolConfiguration.withProxyToServerWorkerThreads(20);

        InetSocketAddress inetSocketAddress = new InetSocketAddress(8888);
        DefaultHttpProxyServer server =
                (DefaultHttpProxyServer) DefaultHttpProxyServer.bootstrap()
                        .withAddress(inetSocketAddress)
                        .withMaxChunkSize(10000)
                        .withAllowRequestToOriginServer(true)
                        .withProxyAlias("LittleProxy")
                        .withThrottling(200000, 2000000)
                        .withThreadPoolConfiguration(threadPoolConfiguration)
                        .withServerResolver(new RedStarHostResolver())
                        .plusActivityTracker(new ActivityTrackerAdapter() {
                            @Override
                            public void requestReceivedFromClient(FlowContext flowContext,
                                                                  HttpRequest httpRequest) {
                                InetSocketAddress clientAddress = flowContext.getClientAddress();
                                logger.info("client address:{}", clientAddress.getAddress().getHostAddress());
                                logger.info("client address:{}", clientAddress.getPort());
                                httpRequest.headers().add("ip", clientAddress.getAddress().getHostAddress());
                            }
                        })
                        .withFiltersSource(new HttpFiltersSourceAdapter() {
                            @Override
                            public int getMaximumRequestBufferSizeInBytes() {
                                return 1024 * 1024 * 30;
                            }

                            @Override
                            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                                return new HttpFiltersAdapter(originalRequest) {
                                    @Override
                                    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                        // TODO: implement your filtering here
                                        ProxyUtils.addVia(originalRequest, "litte-proxy");
                                        if (originalRequest instanceof FullHttpRequest) {
                                            FullHttpRequest fullHttpRequest = (FullHttpRequest) originalRequest;
                                            logger.info("full http request content:{}", new String(fullHttpRequest.content().copy().array()));
                                            logger.info("full http request:{}", new String(fullHttpRequest.duplicate().toString()));
                                            String ip = fullHttpRequest.headers().get("ip");
                                            boolean result = ipRateUtil.getRateConf(fullHttpRequest.getUri(), ip);
                                            if (!result) {
                                                HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
                                                HttpHeaders.setContentLength(httpResponse, 0);
                                                return httpResponse;
                                            }
                                        } else if (originalRequest instanceof DefaultHttpRequest) {
                                            DefaultHttpRequest defaultHttpRequest = (DefaultHttpRequest) originalRequest;
                                        }
                                        return null;
                                    }


                                    @Override
                                    public HttpObject serverToProxyResponse(HttpObject httpObject) {
                                        // TODO: implement your filtering here
                                        if (ProxyUtils.isLastChunk(httpObject)) {

                                        }
                                        return httpObject;
                                    }
                                };
                            }
                        })
                        .start();
    }
}
