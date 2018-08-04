package info.yangguo.waf.request.rewrite;

import com.google.common.collect.Lists;
import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.BasicConfig;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RewriteFilter {
    public static void doFilter(HttpRequest originalRequest, HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            String wafRoute = originalRequest.headers().getAsString(WafHttpHeaderNames.X_WAF_ROUTE);
            String oldUri = originalRequest.uri();

            Map<String, BasicConfig> rewriteConfig = ContextHolder.getClusterService().getRewriteConfigs();
            if (rewriteConfig.containsKey(wafRoute)) {
                String newUri = null;
                for (Map.Entry<String, Object> entry : rewriteConfig.get(wafRoute).getExtension().entrySet()) {
                    Pattern oldUriPattern = Pattern.compile(entry.getKey());
                    Matcher oldUriMatcher = oldUriPattern.matcher(oldUri);
                    if (oldUriMatcher.matches()) {
                        //保存老URI中的匹配组信息，新URI中需要替换使用
                        List<String> oldUriGroups = Lists.newArrayList();
                        for (int i = 1; i < oldUriMatcher.groupCount() + 1; i++) {
                            oldUriGroups.add(oldUriMatcher.group(i));
                        }
                        //获取rewrite config信息
                        newUri = (String) entry.getValue();
                        //老URI没有分组信息，意味着新URI就是目标URI，不需要替换
                        if (oldUriGroups.size() > 0) {
                            //获取newUrlRegex中的需要替换的记号
                            List<Integer> newUriGroup = Lists.newArrayList();
                            Pattern newUriPattern = Pattern.compile("(\\$\\d+)");
                            Matcher newUriMatcher = newUriPattern.matcher(newUri);
                            while (newUriMatcher.find()) {
                                newUriGroup.add(Integer.valueOf(newUriMatcher.group().replace("$", "")));
                            }
                            //找出需要替换的最大值
                            Optional<Integer> max = newUriGroup.parallelStream().max(Integer::compareTo);
                            if (max.isPresent() && max.get() <= oldUriGroups.size()) {
                                for (Integer i : newUriGroup) {
                                    newUri = newUri.replaceAll("\\$" + i, oldUriGroups.get(i-1));
                                }
                            }
                        }
                    }
                }
                if (newUri != null) {
                    originalRequest.setUri(newUri);
                    ((HttpRequest) httpObject).setUri(newUri);
                }
            }
        }
    }
}
