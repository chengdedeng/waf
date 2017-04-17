package com.chinaredstar.waf;

import org.littleshoot.proxy.HttpFiltersAdapter;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
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
    final HttpRequestFilterChain httpRequestFilterChain = new HttpRequestFilterChain()
            .addFilter(new IpHttpRequestFilter());
    final HttpResponseFilterChain httpResponseFilterChain = new HttpResponseFilterChain()
            .addFilter(new ClickjackHttpResponseFilter());

    public RSHttpFilterAdapter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {

        if (httpRequestFilterChain.doFilter(originalRequest)) {
            return create403Response();
        }

        if (originalRequest instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) originalRequest;
        } else if (originalRequest instanceof DefaultHttpRequest) {
            DefaultHttpRequest defaultHttpRequest = (DefaultHttpRequest) originalRequest;
        }
        return null;
    }

    @Override
    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) httpObject;
            String[] hostPort = httpRequest.headers().get("Host").split(":");
            try {
                if (null == Constant.RedStarHostResolver.resolve(hostPort[0], Integer.valueOf(hostPort[1]))) {
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
    public void proxyToServerResolutionSucceeded(String serverHostAndPort,
                                                 InetSocketAddress resolvedRemoteAddress) {
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        if (httpObject instanceof HttpResponse) {
            httpResponseFilterChain.doFilter((HttpResponse) httpObject);
        }
        return httpObject;
    }

    @Override
    public void proxyToServerConnectionSucceeded(ChannelHandlerContext serverCtx) {
        if (isHugeFile(originalRequest.getUri())) {
            ChannelPipeline pipeline = serverCtx.pipeline();
            if (pipeline.get("inflater") != null) {
                pipeline.remove("inflater");
            }
            if (pipeline.get("aggregator") != null) {
                pipeline.remove("aggregator");
            }
        }
        super.proxyToServerConnectionSucceeded(serverCtx);
    }


    private static boolean isHugeFile(String uri) {
        String[] tmp = uri.split("\\?");
        Pattern pattern = Pattern.compile(Constant.hugeFilePattern);
        Matcher matcher = pattern.matcher(tmp[0]);
        return matcher.find();
    }

    private static HttpResponse create403Response() {
        HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
        HttpHeaders.setContentLength(httpResponse, 0);
        return httpResponse;
    }
}
