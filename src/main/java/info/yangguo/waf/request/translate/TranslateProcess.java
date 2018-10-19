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
package info.yangguo.waf.request.translate;

import io.netty.handler.codec.http.HttpResponse;

import java.util.Map;

public interface TranslateProcess {
    /**
     * 需要被处理的waf route正则表达式，匹配则进入该处理器进行处理，真正的处理逻辑
     * 在execute中。
     *
     * @return
     */
    String getWafRoutePattern();

    /**
     * forward处理器
     *
     * @param wafRoute
     * @param uri
     * @param args
     * @return
     */
    HttpResponse execute(String wafRoute, String uri, Map<String, Object> args);
}
