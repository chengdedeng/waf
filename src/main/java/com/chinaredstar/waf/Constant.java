package com.chinaredstar.waf;

/**
 * @author:杨果
 * @date:2017/4/11 下午1:52
 *
 * Description:
 *
 */
public class Constant {
    enum X_Frame_Options {
        DENY,//表示该页面不允许在 frame 中展示,即便是在相同域名的页面中嵌套也不允许.
        SAMEORIGIN//表示该页面可以在相同域名页面的 frame 中展示.
    }

    public static int AcceptorThreads = 5;
    public static int ClientToProxyWorkerThreads = 20;
    public static int ProxyToServerWorkerThreads = 20;
    public static int ServerPort = 8080;
    public static int MaximumRequestBufferSizeInBytes = 0;
    public static int MaximumResponseBufferSizeInBytes = 10 * 1024 * 1024;
    public static RedStarHostResolver RedStarHostResolver = com.chinaredstar.waf.RedStarHostResolver.getInstance();
    public static X_Frame_Options X_Frame_Option = X_Frame_Options.SAMEORIGIN;
}
