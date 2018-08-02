package info.yangguo.waf.model;

public enum RewriteType {
    LAST,//继续后续匹配
    BREAK,//不再后续匹配
    REDIRECT,//302临时重定向
    PERMANET;//301永久重定向
}
