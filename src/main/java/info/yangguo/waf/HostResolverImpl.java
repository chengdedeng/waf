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
package info.yangguo.waf;

import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.ServerConfig;
import org.littleshoot.proxy.HostResolver;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;


/**
 * @author:杨果
 * @date:2017/3/31 下午7:28
 * <p>
 * Description:
 */
class HostResolverImpl implements HostResolver {
    @Override
    public InetSocketAddress resolve(String host, int port)
            throws UnknownHostException {
        String key;
        if (port == 80)
            key = host;
        else
            key = host + ":" + port;
        if (ContextHolder.getClusterService().getUpstreamConfig().containsKey(key)) {
            ServerConfig serverConfig = ContextHolder.getClusterService().getUpstreamConfig().get(key).getServer();
            if (serverConfig != null) {
                return new InetSocketAddress(serverConfig.getIp(), serverConfig.getPort());
            } else {
                throw new UnknownHostException(key + " have not healthy serverConfig.");
            }
        } else {
            throw new UnknownHostException(key);
        }
    }
}
