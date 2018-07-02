package info.yangguo.waf;

import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.model.Server;
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
        String key = host + "_" + port;
        if (ContextHolder.getClusterService().getUpstreamConfig().containsKey(key)) {
            Server server = ContextHolder.getClusterService().getUpstreamConfig().get(key).getServer();
            if (server != null) {
                return new InetSocketAddress(server.getIp(), server.getPort());
            } else {
                throw new UnknownHostException(key + " have not healthy server.");
            }
        } else {
            throw new UnknownHostException(key);
        }
    }
}
