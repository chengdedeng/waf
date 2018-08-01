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
