package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.model.SecurityConfigIterm;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author:杨果
 * @date:2017/5/15 下午2:33
 * <p>
 * Description:
 */
public class PostSecurityFilter extends SecurityFilter {
    private static Logger logger = LoggerFactory.getLogger(PostSecurityFilter.class);

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext ctx, List<SecurityConfigIterm> iterms) {
        if (originalRequest.method().name().equals("POST")) {
            if (httpObject instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) httpObject;
                String contentBody = null;
                List<String> headerValues = Constant.getHeaderValues(originalRequest, "Content-Type");
                if (headerValues.size() > 0 && headerValues.get(0) != null) {
                    if (Constant.getHeaderValues(originalRequest, "Content-Type") != null && headerValues.get(0).startsWith("multipart/form-data")) {
                        contentBody = new String(Unpooled.copiedBuffer(httpContent.content()).array());
                    } else {
                        try {
                            String contentStr = new String(Unpooled.copiedBuffer(httpContent.content()).array()).replaceAll("%", "%25");
                            contentBody = URLDecoder.decode(contentStr, "UTF-8");
                        } catch (Exception e) {
                            logger.warn("URL:{} POST body is inconsistent with the iterms", originalRequest.uri(), e);
                        }
                    }

                    if (contentBody != null) {
                        for (SecurityConfigIterm iterm : iterms) {
                            if (iterm.getConfig().getIsStart()) {
                                Timer itermTimer = Constant.metrics.timer("PostSecurityFilter[" + iterm.getName() + "]");
                                Timer.Context itermContext = itermTimer.time();
                                try {
                                    Pattern pattern = Pattern.compile(iterm.getName());
                                    Matcher matcher = pattern.matcher(contentBody.toLowerCase());
                                    if (matcher.find()) {
                                        hackLog(logger, Constant.getRealIp(originalRequest, ctx), "Post", iterm.getName());
                                        return true;
                                    }
                                } finally {
                                    itermContext.stop();
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
