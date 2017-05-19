package info.yangguo.waf.request;

/**
 * @author:杨果
 * @date:2017/5/11 下午3:57
 *
 * Description:
 *
 */
public enum FilterType {
    ARGS("args"),
    COOKIE("cookie"),
    UA("ua"),
    URL("url"),
    WURL("wurl"),
    POST("post"),
    IP("ip"),
    WIP("wip"),
    FILE("file");

    private String fileName;

    FilterType(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
