package freedom.ava.spider.util;


import com.acxca.components.java.consts.BusinessMessageMap;
import com.acxca.components.java.entity.BusinessMessage;

public class CustomMessageMap extends BusinessMessageMap {
    public static final BusinessMessage SCRAWL_NONE = new BusinessMessage(201, "没有爬取到数据");
    public static final BusinessMessage SCRAWL_FORMAT_WRONG = new BusinessMessage(205, "爬取出错");
    public static final BusinessMessage SCRAWL_FORMAT_WRONG_NO_SPELL = new BusinessMessage(2051, "爬取出错，没有找到单词拼写");
    public static final BusinessMessage SCRAWL_FORMAT_WRONG_NO_MEANING = new BusinessMessage(2052, "爬取出错，没有找到单词意思");
    public static final BusinessMessage SCRAWL_FORMAT_WRONG_NO_EXPLAIN = new BusinessMessage(2053, "爬取出错，没有找到单词释义");
    public static final BusinessMessage SCRAWL_INVALID_PARAM = new BusinessMessage(206, "非法参数");
}
