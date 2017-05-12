package com.chinaredstar.waf;


import org.littleshoot.proxy.ActivityTrackerAdapter;
import org.littleshoot.proxy.FlowContext;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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
        DefaultHttpProxyServer.bootstrap()
                .withAddress(inetSocketAddress)
                .withAllowRequestToOriginServer(true)
                .withProxyAlias("waf")
                .withThreadPoolConfiguration(threadPoolConfiguration)
                .withServerResolver(Constant.RedStarHostResolver)
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
                    //如果需要对请求的报文内容进行过滤,需要启用它从而获得FullHttpRequest,大文件上传的时候得注意这个设置
                    @Override
                    public int getMaximumRequestBufferSizeInBytes() {
                        return Constant.MaximumRequestBufferSizeInBytes;
                    }

                    //Response设置buffer之后,如果碰见大文件下载,必须要inflater和aggregator handler
                    @Override
                    public int getMaximumResponseBufferSizeInBytes() {
                        return Constant.MaximumResponseBufferSizeInBytes;
                    }

                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new RSHttpFilterAdapter(originalRequest, ctx);
                    }
                })
                .start();
    }
}
