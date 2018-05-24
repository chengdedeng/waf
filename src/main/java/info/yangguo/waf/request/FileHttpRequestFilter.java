package info.yangguo.waf.request;

import info.yangguo.waf.Constant;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileHttpRequestFilter extends HttpRequestFilter {
    private static Logger logger = LoggerFactory.getLogger(PostHttpRequestFilter.class);
    private static Pattern filePattern = Pattern.compile("Content-Disposition: form-data;(.+)filename=\"(.+)\\.(.*)\"");

    @Override
    public boolean doFilter(HttpRequest originalRequest, HttpObject httpObject, ChannelHandlerContext ctx, Map<String, Boolean> regexs) {
        if (originalRequest.getMethod().name().equals("POST")) {
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
                            logger.warn("URL:{} POST body is inconsistent with the rules", originalRequest.getUri(), e);
                        }
                    }

                    if (contentBody != null) {
                        Matcher fileMatcher = filePattern.matcher(contentBody);
                        if (fileMatcher.find()) {
                            String fileExt = fileMatcher.group(3);
                            for (Map.Entry<String, Boolean> regex : regexs.entrySet()) {
                                if (regex.getValue()) {
                                    Pattern pattern = Pattern.compile(regex.getKey());
                                    if (pattern.matcher(fileExt).matches()) {
                                        hackLog(logger, Constant.getRealIp(originalRequest, ctx), "File", regex.getKey());
                                        return true;
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
