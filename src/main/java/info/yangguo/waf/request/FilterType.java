package info.yangguo.waf.request;

/**
 * @author:杨果
 * @date:2017/5/11 下午3:57
 *
 * Description:
 *
 */
public enum FilterType {
    ARGS("args.txt"),
    COOKIE("cookie.txt"),
    UA("ua.txt"),
    URL("url.txt"),
    WURL("wurl.txt"),
    POST("post.txt"),
    IP("ip.txt"),
    WIP("wip.txt"),
    FILE("file.txt");

    private String fileName;

    FilterType(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
