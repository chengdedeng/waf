package info.yangguo.waf.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public enum RewriteType {
    LAST("last", "继续后续匹配"),
    BREAK("break", "不再后续匹配"),
    REDIRECT("redirect", "302临时重定向"),
    PERMANET("permanet", "301永久重定向");

    @Getter
    private String code;
    @Getter
    private String desc;

    RewriteType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static Optional<RewriteType> getByCode(String code) {
        AtomicReference<Optional<RewriteType>> optional = new AtomicReference<>(Optional.empty());
        Arrays.stream(RewriteType.values()).anyMatch(type -> {
            if (type.getCode().equals(code)) {
                optional.set(Optional.of(type));
                return true;
            } else {
                return false;
            }
        });
        return optional.get();
    }
}
