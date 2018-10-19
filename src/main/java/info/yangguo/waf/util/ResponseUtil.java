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
package info.yangguo.waf.util;

import io.netty.handler.codec.http.*;

public class ResponseUtil {
    public static HttpResponse createResponse(HttpResponseStatus httpResponseStatus, HttpRequest originalRequest, HttpHeaders httpHeaders) {
        if (httpHeaders == null)
            httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        httpHeaders.add(HttpHeaderNames.CONNECTION, "close");//如果不关闭，下游的server接收到部分数据会一直等待知道超时，会报如下大概异常
        //I/O error while reading input message; nested exception is java.net.SocketTimeoutException
        HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpResponseStatus);

        //support CORS
        String origin = originalRequest.headers().getAsString(HttpHeaderNames.ORIGIN);
        if (origin != null) {
            httpHeaders.set("Access-Control-Allow-Credentials", "true");
            httpHeaders.set("Access-Control-Allow-Origin", origin);
        }
        httpResponse.headers().add(httpHeaders);
        return httpResponse;
    }
}
