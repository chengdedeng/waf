package com.chinaredstar.waf;

import com.chinaredstar.waf.util.PropertiesUtil;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * @author:杨果
 * @date:2017/4/11 下午1:52
 *
 * Description:
 *
 */
public class Constant {
    enum X_Frame_Options {
        DENY,//表示该页面不允许在 frame 中展示,即便是在相同域名的页面中嵌套也不允许.
        SAMEORIGIN//表示该页面可以在相同域名页面的 frame 中展示.
    }

    public static int AcceptorThreads = 5;
    public static int ClientToProxyWorkerThreads = 20;
    public static int ProxyToServerWorkerThreads = 20;
    public static int ServerPort = 8080;
    public static RedStarHostResolver RedStarHostResolver = com.chinaredstar.waf.RedStarHostResolver.getInstance();
    public static X_Frame_Options X_Frame_Option = X_Frame_Options.SAMEORIGIN;
    private static Pattern ipv4Pattern = Pattern.compile("^(?:/)(((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?))(?::\\d{1,5}$)");
    private static Pattern ipv6Pattern = Pattern.compile("^(?:/)(\\s*((([0-9A-Fa-f]{1,4}:){7}(([0-9A-Fa-f]{1,4})|:))|(([0-9A-Fa-f]{1,4}:){6}(:|((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})|(:[0-9A-Fa-f]{1,4})))|(([0-9A-Fa-f]{1,4}:){5}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4}){0,1}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){0,2}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:)(:[0-9A-Fa-f]{1,4}){0,4}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(:(:[0-9A-Fa-f]{1,4}){0,5}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})))(%.+)?\\s*)(?::\\d{1,5}$)");
    public static Map<String, String> wafConfs = PropertiesUtil.getProperty("waf.properties");

    public static String getRealIp(HttpRequest httpRequest, ChannelHandlerContext channelHandlerContext) {
        String xRealIP = httpRequest.headers().get("X-Real-IP");
        String remoteAddress = channelHandlerContext.channel().remoteAddress().toString();
        String realIp = null;
        if (xRealIP != null) {
            realIp = xRealIP;
        } else {
            Matcher matcher1 = ipv4Pattern.matcher(remoteAddress);
            if (matcher1.find()) {
                realIp = matcher1.group(1);
            } else {
                Matcher matcher2 = ipv6Pattern.matcher(remoteAddress);
                if (matcher2.find()) {
                    realIp = matcher2.group(2);
                }
            }
            httpRequest.headers().add("X-Real-IP", realIp);
        }
        return realIp;
    }
}
