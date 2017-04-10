package com.chinaredstar.waf.config;

import com.baidu.disconf.client.common.annotations.DisconfFile;
import com.baidu.disconf.client.common.annotations.DisconfFileItem;

import org.springframework.stereotype.Component;

/**
 * @author:杨果
 * @date:2017/4/5 下午7:06
 *
 * Description:
 *
 */
@Component("ipRateConf")
@DisconfFile(filename = "ipRate.properties")
public class IpRateConf implements Cloneable {
    private String s;
    private String m;
    private String h;
    private String d;


    @DisconfFileItem(name = "ip.rate.limit.s")
    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    @DisconfFileItem(name = "ip.rate.limit.m")
    public String getM() {
        return m;
    }

    public void setM(String m) {
        this.m = m;
    }

    @DisconfFileItem(name = "ip.rate.limit.h")
    public String getH() {
        return h;
    }

    public void setH(String h) {
        this.h = h;
    }

    @DisconfFileItem(name = "ip.rate.limit.d")
    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return "IpRateConf{" +
                "s='" + s + '\'' +
                ", m='" + m + '\'' +
                ", h='" + h + '\'' +
                ", d='" + d + '\'' +
                '}';
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
