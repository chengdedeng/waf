package com.chinaredstar.waf;

import com.chinaredstar.waf.request.ArgsHttpRequestFilter;
import com.chinaredstar.waf.request.CookieHttpRequestFilter;
import com.chinaredstar.waf.request.HttpRequestFilterChain;
import com.chinaredstar.waf.request.IpHttpRequestFilter;
import com.chinaredstar.waf.request.UaHttpRequestFilter;
import com.chinaredstar.waf.request.UrlHttpRequestFilter;
import com.chinaredstar.waf.request.WIpHttpRequestFilter;
import com.chinaredstar.waf.response.ClickjackHttpResponseFilter;
import com.chinaredstar.waf.response.HttpResponseFilterChain;

import org.littleshoot.proxy.HttpFiltersAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

/**
 * @author:杨果
 * @date:2017/4/17 下午2:12
 *
 * Description:
 *
 */
public class RSHttpFilterAdapter extends HttpFiltersAdapter {
    private static Logger logger = LoggerFactory.getLogger(RSHttpFilterAdapter.class);
    private final HttpRequestFilterChain httpRequestFilterChain = new HttpRequestFilterChain()
            .addFilter(new WIpHttpRequestFilter())
            .addFilter(new IpHttpRequestFilter())
            .addFilter(new ArgsHttpRequestFilter())
            .addFilter(new UrlHttpRequestFilter())
            .addFilter(new CookieHttpRequestFilter())
            .addFilter(new UaHttpRequestFilter());
    private final HttpResponseFilterChain httpResponseFilterChain = new HttpResponseFilterChain()
            .addFilter(new ClickjackHttpResponseFilter());

    private final static ThreadLocal<HttpPostRequestDecoder> httpPostRequestDecoderThreadLocal = new ThreadLocal<>();

    public RSHttpFilterAdapter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
    }


    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        //黑白名单
        //URI过滤
        //参数过滤

        if (originalRequest.getMethod().name().equals("POST")) {
            if (httpPostRequestDecoderThreadLocal.get() == null) {
                httpPostRequestDecoderThreadLocal.set(new HttpPostRequestDecoder(originalRequest));
            }
            if (httpObject instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) httpObject;
                HttpContent httpContent1 = httpContent.copy();
                byte[] bytes = Unpooled.copiedBuffer(httpContent1.content()).array();
                System.out.println(new String(bytes));
            }
            if (httpObject instanceof LastHttpContent) {
                httpPostRequestDecoderThreadLocal.remove();
            }
        } else {
            DefaultHttpRequest defaultHttpRequest = (DefaultHttpRequest) originalRequest;
            String uri = defaultHttpRequest.getUri();
        }

        if (httpRequestFilterChain.doFilter(originalRequest, ctx)) {
            return create403Response();
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
