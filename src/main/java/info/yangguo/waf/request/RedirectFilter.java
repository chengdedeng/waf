/*
 * Copyright 2018-present yangguo@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.yangguo.waf.request;

import info.yangguo.waf.WafHttpHeaderNames;
import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.BasicConfig;
import info.yangguo.waf.util.ResponseUtil;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.CharEncoding.UTF_8;

public class RedirectFilter implements RequestFilter {
    private static Logger LOGGER = LoggerFactory.getLogger(RedirectFilter.class);

    @Override
    public HttpResponse doFilter(HttpRequest originalRequest, HttpObject httpObject) {
        HttpResponse httpResponse = null;
        if (httpObject instanceof HttpRequest) {
            String wafRoute = originalRequest.headers().getAsString(WafHttpHeaderNames.X_WAF_ROUTE);
            String redirectUri = originalRequest.uri();
            try {
                redirectUri = UriUtils.decode(originalRequest.uri(), UTF_8);
            } catch (Exception e) {
                LOGGER.warn("uri decode is failed.", e);
            }

            Map<String, BasicConfig> redirectConfig = ContextHolder.getClusterService().getRedirectConfigs();
            if (redirectConfig.containsKey(wafRoute) && redirectConfig.get(wafRoute).getIsStart()) {
                for (Map.Entry<String, Object> entry : redirectConfig.get(wafRoute).getExtension().entrySet()) {
                    Pattern redirectPattern = Pattern.compile(entry.getKey());
                    Matcher redirectMatcher = redirectPattern.matcher(redirectUri);
                    if (redirectMatcher.matches()) {
                        String[] parts = ((String) entry.getValue()).split(" +");
                        HttpHeaders httpHeaders = new DefaultHttpHeaders();
                        httpHeaders.add(HttpHeaderNames.LOCATION, parts[0] + originalRequest.uri());
                        httpResponse = ResponseUtil.createResponse(HttpResponseStatus.valueOf(Integer.valueOf(parts[1])), originalRequest, httpHeaders);
                    }
                }
            }
        }
        return httpResponse;
    }
}

