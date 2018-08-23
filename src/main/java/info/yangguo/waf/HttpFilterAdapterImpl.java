package info.yangguo.waf;

import com.google.common.collect.Lists;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.ForwardConfig;
import info.yangguo.waf.model.WeightedRoundRobinScheduling;
import info.yangguo.waf.request.*;
import info.yangguo.waf.response.HttpResponseFilter;
import info.yangguo.waf.util.ResponseUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.impl.ClientToProxyConnection;
import org.littleshoot.proxy.impl.ProxyConnection;
import org.littleshoot.proxy.impl.ProxyToServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * @author:杨果
 * @date:2017/4/17 下午2:12
 * <p>
 * Description:
 */
public class HttpFilterAdapterImpl extends HttpFiltersAdapter {
    private static Logger logger = LoggerFactory.getLogger(HttpFilterAdapterImpl.class);

    public HttpFilterAdapterImpl(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        //放到里面主要是为了线程安全，由于一条链路不断的情况下，多个请求过来都在一个ClientToProxy线程中，但是对于Filter来说确实多线程处理的，
        //不放在里面就会报对List操作的操作异常。
        List<RequestFilter> requestFilters = Lists.newArrayList();
        //注意顺序
        requestFilters.add(new RewriteFilter());
        requestFilters.add(new RedirectFilter());
        requestFilters.add(new SecurityFilter());
        requestFilters.add(new TranslateFilter());

        HttpResponse response = null;
        for (RequestFilter filter : requestFilters) {
            try {
                response = filter.doFilter(originalRequest, httpObject);
            } catch (Exception e) {
                response = ResponseUtil.createResponse(HttpResponseStatus.BAD_GATEWAY, originalRequest, null);
            }
            if (response != null) {
                break;
            }
        }

        return response;
    }

    @Override
    public void proxyToServerResolutionSucceeded(String serverHostAndPort,
                                                 InetSocketAddress resolvedRemoteAddress) {
        if (resolvedRemoteAddress == null) {
            //在使用 Channel 写数据之前，建议使用 isWritable() 方法来判断一下当前 ChannelOutboundBuffer 里的写缓存水位，防止 OOM 发生。不过实践下来，正常的通信过程不太会 OOM，但当网络环境不好，同时传输报文很大时，确实会出现限流的情况。
            if (ctx.channel().isWritable()) {
                ctx.writeAndFlush(ResponseUtil.createResponse(HttpResponseStatus.BAD_GATEWAY, originalRequest, null));
            }
        }
    }

    /**
     * <b>Important:</b>：这个只能用在HTTP1.1上
     * 浏览器->Nginx->Waf->Tomcat，如果Nginx->Waf是Http1.0，那么Waf->Tomcat之间的链路会自动关闭，而关闭之时，Waf有可能还没有将报文返回给Nginx，所以
     * Nginx上会有大量的<b>upstream prematurely closed connection while reading upstream</b>异常！这样设计的前提是，waf->server的链接关闭只有两种情况
     * <p>
     * 1. idle超时关闭。
     * <p>
     * 2. 异常关闭，例如大文件上传超过tomcat中程序允许上传的最大值，并且tomcat未设置maxswallow时，从而导致tomcat发送RST。
     * <p>
     * 代理链接的是两个或多个使用相同协议的应用程序，此处的相同非常重要，所以中间最少别随意跟换协议！！
     */
    @Override
    public void proxyToServerRequestSending() {
        ClientToProxyConnection clientToProxyConnection = (ClientToProxyConnection) ctx.handler();
        ProxyConnection proxyConnection = clientToProxyConnection.getProxyToServerConnection();
        logger.debug("client channel:{}-{}", clientToProxyConnection.getChannel().localAddress().toString(), clientToProxyConnection.getChannel().remoteAddress().toString());
        logger.debug("server channel:{}-{}", proxyConnection.getChannel().localAddress().toString(), proxyConnection.getChannel().remoteAddress().toString());
        proxyConnection.getChannel().closeFuture().addListener(new GenericFutureListener() {
            @Override
            public void operationComplete(Future future) {
                if (clientToProxyConnection.getChannel().isActive()) {
                    logger.debug("channel:{}-{} will be closed", clientToProxyConnection.getChannel().localAddress().toString(), clientToProxyConnection.getChannel().remoteAddress().toString());
                    clientToProxyConnection.getChannel().close();
                } else {
                    logger.debug("channel:{}-{} has been closed", clientToProxyConnection.getChannel().localAddress().toString(), clientToProxyConnection.getChannel().remoteAddress().toString());
                }
            }
        });
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        HttpResponseFilter httpResponseFilter = new HttpResponseFilter();
        if (httpObject instanceof HttpResponse) {
            try {
                httpResponseFilter.doFilter(originalRequest, (HttpResponse) httpObject, ContextHolder.getClusterService());
            } catch (Exception e) {
                logger.error("response filter failed", e.getCause());
            }
        }
        return httpObject;
    }

    @Override
    public void proxyToServerConnectionFailed() {
        if ("on".equals(Constant.wafConfs.get("waf.lb"))) {
            try {
                ClientToProxyConnection clientToProxyConnection = (ClientToProxyConnection) ctx.handler();
                ProxyToServerConnection proxyToServerConnection = clientToProxyConnection.getProxyToServerConnection();

                String serverHostAndPort = proxyToServerConnection.getServerHostAndPort().replace(":", "_");

                String remoteHostName = proxyToServerConnection.getRemoteAddress().getAddress().getHostAddress();
                int remoteHostPort = proxyToServerConnection.getRemoteAddress().getPort();

                WeightedRoundRobinScheduling weightedRoundRobinScheduling = ContextHolder.getClusterService().getUpstreamConfig().get(serverHostAndPort);
                weightedRoundRobinScheduling.getUnhealthilyServerConfigs().add(weightedRoundRobinScheduling.getServersMap().get(remoteHostName + "_" + remoteHostPort));
                weightedRoundRobinScheduling.getHealthilyServerConfigs().remove(weightedRoundRobinScheduling.getServersMap().get(remoteHostName + "_" + remoteHostPort));
            } catch (Exception e) {
                logger.error("connection of proxy->server is failed", e);
            }
        }
    }

    @Override
    public void proxyToServerConnectionSucceeded(final ChannelHandlerContext serverCtx) {
        Map<String, ForwardConfig> forwardConfigMap = ContextHolder.getClusterService().getTranslateConfigs();
        //forward的时候牵涉到协议转换，所以必须要是FullHttpRequest，所以我们必须要使用aggregator
        if (!forwardConfigMap.containsKey(originalRequest.headers().getAsString(WafHttpHeaderNames.X_WAF_ROUTE))) {
            ChannelPipeline pipeline = serverCtx.pipeline();
            //当没有修改getMaximumResponseBufferSizeInBytes中buffer默认的大小时,下面两个handler是不存在的
            if (pipeline.get("inflater") != null) {
                pipeline.remove("inflater");
            }
            if (pipeline.get("aggregator") != null) {
                pipeline.remove("aggregator");
            }
        }
        super.proxyToServerConnectionSucceeded(serverCtx);
    }
}
