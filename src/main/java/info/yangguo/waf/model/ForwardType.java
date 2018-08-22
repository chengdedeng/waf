package info.yangguo.waf.model;

public enum ForwardType {
    HTTP,
    DUBBO,
    GRPC,
    THRIFT,
    SOFA;

    public static ForwardType getType(String name) {
        for (ForwardType type : ForwardType.values()) {
            if (type.name().equals(name))
                return type;
        }
        return null;
    }
}
