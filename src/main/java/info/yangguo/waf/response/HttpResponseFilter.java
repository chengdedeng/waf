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
package info.yangguo.waf.response;

import info.yangguo.waf.model.ResponseConfig;
import info.yangguo.waf.service.ClusterService;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @author:杨果
 * @date:2017/4/11 下午4:45
 * <p>
 * Description:
 */
public class HttpResponseFilter {
    public List<ResponseProcess> filters = new ArrayList<>();

    public HttpResponseFilter() {
        filters.add(new ClickjackResponseProcess());
    }

    public void doFilter(HttpRequest originalRequest, HttpResponse httpResponse, ClusterService clusterService) {
        for (ResponseProcess filter : filters) {
            ResponseConfig responseConfig = clusterService.getResponseConfigs().get(filter.getClass().getName());
            if (responseConfig.getConfig().getIsStart()) {
                filter.doFilter(originalRequest, httpResponse);
            }
        }
    }
}
