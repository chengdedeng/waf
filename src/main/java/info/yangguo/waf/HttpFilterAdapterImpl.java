package info.yangguo.waf;

import info.yangguo.waf.request.*;
import info.yangguo.waf.response.ClickjackHttpResponseFilter;
import info.yangguo.waf.response.HttpResponseFilterChain;
import info.yangguo.waf.util.WeightedRoundRobinScheduling;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.impl.ClientToProxyConnection;
import org.littleshoot.proxy.impl.ProxyToServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author:杨果
 * @date:2017/4/17 下午2:12
 * <p>
 * Description:
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
        if (Constant.wafConfs.get("waf.url.blacklist").equals("on")) {
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
            ImmutablePair<Boolean, HttpRequestFilter> immutablePair = httpRequestFilterChain.doFilter(originalRequest, httpObject, ctx);
            if (immutablePair.left) {
                ctx.writeAndFlush(createResponse(HttpResponseStatus.FORBIDDEN, originalRequest), channelPromise);
            }
        } catch (Exception e) {
            ctx.writeAndFlush(createResponse(HttpResponseStatus.BAD_GATEWAY, originalRequest), channelPromise);
            logger.error("client's request failed", e);
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
            ctx.writeAndFlush(createResponse(HttpResponseStatus.BAD_GATEWAY, originalRequest), channelPromise);
        }
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        if (httpObject instanceof HttpResponse) {
            httpResponseFilterChain.doFilter(originalRequest, (HttpResponse) httpObject);
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

    private static HttpResponse createResponse(HttpResponseStatus httpResponseStatus, HttpRequest originalRequest) {
        HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpResponseStatus);
        //support CORS
        List<String> originHeader = Constant.getHeaderValues(originalRequest, "Origin");
        if (originHeader.size() > 0) {
            HttpHeaders httpHeaders = new DefaultHttpHeaders();
            httpHeaders.set("Access-Control-Allow-Credentials", "true");
            httpHeaders.set("Access-Control-Allow-Origin", originHeader.get(0));
            httpResponse.headers().add(httpHeaders);
        }
        return httpResponse;
    }
}
