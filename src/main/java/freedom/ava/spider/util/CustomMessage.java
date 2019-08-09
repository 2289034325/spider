package freedom.ava.spider.util;

public class CustomMessage {
    private int id;
    private String msg;
    public CustomMessage(int id, String msg) {
        this.id = id;
        this.msg = msg;
    }

    public int getId() {
        return id;
    }

    public String getMsg() {
        return msg;
    }
}
