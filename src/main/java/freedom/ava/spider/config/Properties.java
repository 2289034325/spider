package freedom.ava.spider.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Properties{

    @Value("${spider.phantomjs.path}")
    private String phantomjsPath;

    @Value("${spider.hj.english.url}")
    private String hjen;
    @Value("${spider.hj.japanese.url}")
    private String hjjp;
    @Value("${spider.hj.korean.url}")
    private String hjkr;
    @Value("${spider.hj.french.url}")
    private String hjfr;

    public String getPhantomjsPath() {
        return phantomjsPath;
    }

    public void setPhantomjsPath(String phantomjsPath) {
        this.phantomjsPath = phantomjsPath;
    }

    public String getHjen() {
        return hjen;
    }

    public void setHjen(String hjen) {
        this.hjen = hjen;
    }

    public String getHjjp() {
        return hjjp;
    }

    public void setHjjp(String hjjp) {
        this.hjjp = hjjp;
    }

    public String getHjkr() {
        return hjkr;
    }

    public void setHjkr(String hjkr) {
        this.hjkr = hjkr;
    }

    public String getHjfr() {
        return hjfr;
    }

    public void setHjfr(String hjfr) {
        this.hjfr = hjfr;
    }
}
