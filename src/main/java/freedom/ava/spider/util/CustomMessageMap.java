package freedom.ava.spider.util;


public class CustomMessageMap extends BaseCustomMessageMap {
    public static final CustomMessage SCRAWL_NONE = new CustomMessage(201, "没有爬取到数据");
    public static final CustomMessage WORD_EXIST = new CustomMessage(204, "存在相同的单词");
    public static final CustomMessage SCRAWL_FORMAT_WRONG = new CustomMessage(205, "爬取出错");
    public static final CustomMessage SCRAWL_INVALID_PARAM = new CustomMessage(206, "非法参数");
}
