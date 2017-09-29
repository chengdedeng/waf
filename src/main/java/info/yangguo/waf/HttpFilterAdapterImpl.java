package info.yangguo.waf;

import info.yangguo.waf.request.ArgsHttpRequestFilter;
import info.yangguo.waf.request.CCHttpRequestFilter;
import info.yangguo.waf.request.CookieHttpRequestFilter;
import info.yangguo.waf.request.HttpRequestFilterChain;
import info.yangguo.waf.request.IpHttpRequestFilter;
import info.yangguo.waf.request.PostHttpRequestFilter;
import info.yangguo.waf.request.ScannerHttpRequestFilter;
import info.yangguo.waf.request.UaHttpRequestFilter;
import info.yangguo.waf.request.UrlHttpRequestFilter;
import info.yangguo.waf.request.WIpHttpRequestFilter;
import info.yangguo.waf.request.WUrlHttpRequestFilter;
import info.yangguo.waf.response.ClickjackHttpResponseFilter;
import info.yangguo.waf.response.HttpResponseFilterChain;
import info.yangguo.waf.util.WeightedRoundRobinScheduling;

import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.impl.ClientToProxyConnection;
import org.littleshoot.proxy.impl.ProxyToServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author:杨果
 * @date:2017/4/17 下午2:12
 *
 * Description:
 *
 */
public class HttpFilterAdapterImpl extends HttpFiltersAdapter {
    private static Logger logger = LoggerFactory.getLogger(HttpFilterAdapterImpl.class);
    private static final HttpRequestFilterChain httpRequestFilterChain = new HttpRequestFilterChain();
    private final HttpResponseFilterChain httpResponseFilterChain = new HttpResponseFilterChain()
            .addFilter(new ClickjackHttpResponseFilter());

    static {
        if (Constant.wafConfs.get("waf.ip.whitelist").equals("on")) {
            httpRequestFilterChain.addFilter(new WIpHttpRequestFilter());
        }
        if (Constant.wafConfs.get("waf.ip.blacklist").equals("on")) {
            httpRequestFilterChain.addFilter(new IpHttpRequestFilter());
        }
        if (Constant.wafConfs.get("waf.cc").equals("on")) {
            httpRequestFilterChain.addFilter(new CCHttpRequestFilter());
        }
        if (Constant.wafConfs.get("waf.scanner").equals("on")) {
            httpRequestFilterChain.addFilter(new ScannerHttpRequestFilter());
        }
        if (Constant.wafConfs.get("waf.url.whitelist").equals("on")) {
            httpRequestFilterChain.addFilter(new WUrlHttpRequestFilter());
        }
        if (Constant.wafConfs.get("waf.ua").equals("on")) {
            httpRequestFilterChain.addFilter(new UaHttpRequestFilter());
        }
        if (Constant.wafConfs.get("waf.url").equals("on")) {
            httpRequestFilterChain.addFilter(new UrlHttpRequestFilter());
        }
        if (Constant.wafConfs.get("waf.args").equals("on")) {
            httpRequestFilterChain.addFilter(new ArgsHttpRequestFilter());
        }
        if (Constant.wafConfs.get("waf.cookie").equals("on")) {
            httpRequestFilterChain.addFilter(new CookieHttpRequestFilter());
        }
        if (Constant.wafConfs.get("waf.post").equals("on")) {
            httpRequestFilterChain.addFilter(new PostHttpRequestFilter());
        }
    }


    public HttpFilterAdapterImpl(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
    }


    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        ChannelPromise channelPromise = ctx.newPromise().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isDone()) {
                    ctx.close();
                }
            }
        });
        try {
            if (httpRequestFilterChain.doFilter(originalRequest, httpObject, ctx)) {
                ctx.writeAndFlush(create403Response(), channelPromise);
            }
        } catch (Exception e) {
            ctx.writeAndFlush(create502Response(), channelPromise);
            logger.error("client's request failed",e);
        }
        return null;
    }

    @Override
    public void proxyToServerResolutionSucceeded(String serverHostAndPort,
                                                 InetSocketAddress resolvedRemoteAddress) {
        if (resolvedRemoteAddress == null) {
            ChannelPromise channelPromise = ctx.newPromise().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isDone()) {
                        ctx.close();
                    }
                }
            });
            ctx.writeAndFlush(create502Response(), channelPromise);
        }
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        if (httpObject instanceof HttpResponse) {
            httpResponseFilterChain.doFilter((HttpResponse) httpObject);
        }
        return httpObject;
    }

    @Override
    public void proxyToServerConnectionFailed() {
        if ("on".equals(Constant.wafConfs.get("waf.proxy.lb"))) {
            ClientToProxyConnection clientToProxyConnection = (ClientToProxyConnection) ctx.handler();
            try {
                Field field = ClientToProxyConnection.class.getDeclaredField("currentServerConnection");
                field.setAccessible(true);
                ProxyToServerConnection proxyToServerConnection = (ProxyToServerConnection) field.get(clientToProxyConnection);

                String serverHostAndPort = proxyToServerConnection.getServerHostAndPort().replace(":", "_");

                String remoteHostName = proxyToServerConnection.getRemoteAddress().getAddress().getHostAddress();
                int remoteHostPort = proxyToServerConnection.getRemoteAddress().getPort();

                WeightedRoundRobinScheduling weightedRoundRobinScheduling = HostResolverImpl.getSingleton().getServers(serverHostAndPort);
                weightedRoundRobinScheduling.unhealthilyServers.add(weightedRoundRobinScheduling.serversMap.get(remoteHostName + "_" + remoteHostPort));
                weightedRoundRobinScheduling.healthilyServers.remove(weightedRoundRobinScheduling.serversMap.get(remoteHostName + "_" + remoteHostPort));
            } catch (Exception e) {
                logger.error("connection of proxy->server is failed", e);
            }
        }
    }

    @Override
    public void proxyToServerConnectionSucceeded(final ChannelHandlerContext serverCtx) {
        ChannelPipeline pipeline = serverCtx.pipeline();
        //当没有修改getMaximumResponseBufferSizeInBytes中buffer默认的大小时,下面两个handler是不存在的
        if (pipeline.get("inflater") != null) {
            pipeline.remove("inflater");
        }
        if (pipeline.get("aggregator") != null) {
            pipeline.remove("aggregator");
        }
        super.proxyToServerConnectionSucceeded(serverCtx);
    }

    private static HttpResponse create403Response() {
        HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
        HttpHeaders.setContentLength(httpResponse, 0);
        return httpResponse;
    }

    private static HttpResponse create502Response() {
        HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
        HttpHeaders.setContentLength(httpResponse, 0);
        return httpResponse;
    }
}
