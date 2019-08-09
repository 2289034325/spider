package freedom.ava.spider.util;

public class BusinessException extends RuntimeException  {

    private CustomMessage msg;

    public BusinessException(CustomMessage msg){
        super(msg.getMsg());
        this.msg = msg;
    }

    public CustomMessage getMsg() {
        return msg;
    }
}
