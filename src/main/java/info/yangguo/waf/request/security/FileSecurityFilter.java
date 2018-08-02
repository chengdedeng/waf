package info.yangguo.waf.request.security;

import com.codahale.metrics.Timer;
import info.yangguo.waf.Constant;
import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.model.SecurityConfigIterm;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileSecurityFilter extends SecurityFilter {
    private static Logger logger = LoggerFactory.getLogger(PostSecurityFilter.class);
    private static Pattern filePattern = Pattern.compile("Content-Disposition: form-data;(.+)filename=\"(.+)\\.(.*)\"");

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext ctx, List<SecurityConfigIterm> iterms) {
        if (originalRequest.method().name().equals("POST")) {
            if (httpObject instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) httpObject;
                String contentBody = null;
                String contentType = originalRequest.headers().getAsString(HttpHeaderNames.CONTENT_TYPE);
                if (contentType != null) {
                    if (contentType.startsWith("multipart/form-data")) {
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
                        Matcher fileMatcher = filePattern.matcher(contentBody);
                        if (fileMatcher.find()) {
                            String fileExt = fileMatcher.group(3);
                            for (SecurityConfigIterm iterm : iterms) {
                                if (iterm.getConfig().getIsStart()) {
                                    Timer itermTimer = Constant.metrics.timer("FileSecurityFilter[" + iterm.getName() + "]");
                                    Timer.Context itermContext = itermTimer.time();
                                    try {
                                        Pattern pattern = Pattern.compile(iterm.getName());
                                        if (pattern.matcher(fileExt).matches()) {
                                            hackLog(logger, originalRequest.headers().getAsString(WafHttpHeaderNames.X_REAL_IP), "File", iterm.getName());
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
        }
        return false;
    }
}
