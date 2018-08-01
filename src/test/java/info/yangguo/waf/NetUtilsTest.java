package info.yangguo.waf;

import info.yangguo.waf.util.NetUtils;
import org.junit.Test;

import java.net.InetAddress;

/**
 * Created with IntelliJ IDEA.
 * User: guo
 * Date: 2017/10/25
 * Time: 下午4:41
 * Description:
 */
public class NetUtilsTest {
    @Test
    public void getLocalAddress() {
        InetAddress inetAddress = NetUtils.getLocalAddress();
        System.out.println(inetAddress.getHostAddress());
        System.out.println(inetAddress.getHostName());
        System.out.println(inetAddress.getCanonicalHostName());
    }

    @Test
    public void getLocalHost() {
        String localHost = NetUtils.getLocalHost();
        System.out.println(localHost);
    }
}
