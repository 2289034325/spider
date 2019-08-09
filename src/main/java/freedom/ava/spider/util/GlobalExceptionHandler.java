package freedom.ava.spider.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URLEncoder;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有业务异常
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    ResponseEntity handleBusinessException(BusinessException e){
        HttpHeaders headers = new HttpHeaders();
        headers.add("msg-id", String.valueOf(e.getMsg().getId()));
        try {
            headers.add("msg-content", URLEncoder.encode(e.getMsg().getMsg(), "utf-8"));
        }catch (Exception ex){

        }
        ResponseEntity res = new ResponseEntity(headers,HttpStatus.NO_CONTENT);

        return res;
    }
}
