package info.yangguo.waf;

import io.netty.util.AsciiString;

public class WafHttpHeaderNames {
    public static final AsciiString X_REAL_IP = AsciiString.cached("x-real-ip");
    public static final AsciiString X_WAF_ROUTE = AsciiString.cached("x-waf-route");
    public static final AsciiString X_FORWARDED_FOR = AsciiString.cached("X-Forwarded-For");
}
