package com.chinaredstar.waf;

import com.chinaredstar.waf.request.ArgsHttpRequestFilter;
import com.chinaredstar.waf.request.CCHttpRequestFilter;
import com.chinaredstar.waf.request.CookieHttpRequestFilter;
import com.chinaredstar.waf.request.HttpRequestFilterChain;
import com.chinaredstar.waf.request.IpHttpRequestFilter;
import com.chinaredstar.waf.request.PostHttpRequestFilter;
import com.chinaredstar.waf.request.ScannerHttpRequestFilter;
import com.chinaredstar.waf.request.UaHttpRequestFilter;
import com.chinaredstar.waf.request.UrlHttpRequestFilter;
import com.chinaredstar.waf.request.WIpHttpRequestFilter;
import com.chinaredstar.waf.request.WUrlHttpRequestFilter;
import com.chinaredstar.waf.response.ClickjackHttpResponseFilter;
import com.chinaredstar.waf.response.HttpResponseFilterChain;

import org.littleshoot.proxy.HttpFiltersAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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
public class RSHttpFilterAdapter extends HttpFiltersAdapter {
    private static Logger logger = LoggerFactory.getLogger(RSHttpFilterAdapter.class);
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


    public RSHttpFilterAdapter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
    }


    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpRequestFilterChain.doFilter(originalRequest, httpObject, ctx)) {
            ChannelPromise channelPromise = ctx.newPromise().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture arg0) throws Exception {
                    if (arg0.isDone()) {
                        ctx.close();
                    }
                }
            });
            ctx.writeAndFlush(create403Response(), channelPromise);
        }
        return null;
    }

    @Override
    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) httpObject;
            String[] hostPort = httpRequest.headers().get("Host").split(":");
            try {
                if (null == Constant.RedStarHostResolver.resolve(hostPort[0], hostPort.length == 2 ? Integer.valueOf(hostPort[1]) : 80)) {
                    HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
                    HttpHeaders.setContentLength(httpResponse, 0);
                    return httpResponse;
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        if (httpObject instanceof HttpResponse) {
            httpResponseFilterChain.doFilter((HttpResponse) httpObject);
        }
        return httpObject;
    }

    @Override
    public InetSocketAddress proxyToServerResolutionStarted(
            String resolvingServerHostAndPort) {
        return super.proxyToServerResolutionStarted(resolvingServerHostAndPort);
    }

    @Override
    public void proxyToServerConnectionFailed() {
    }


    @Override
    public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
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
}
