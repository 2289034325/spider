package freedom.ava.spider.api;

import freedom.ava.spider.entity.VocabularyMessage;
import freedom.ava.spider.entity.Word;
import freedom.ava.spider.repository.DictionaryRepository;
import freedom.ava.spider.service.SpiderService;
import freedom.ava.spider.util.BusinessException;
import freedom.ava.spider.util.CustomMessageMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 爬单词
 */

@RestController
@RequestMapping(path = "vocabulary",produces= MediaType.APPLICATION_JSON_VALUE)
public class VocabularyController {

    @Autowired
    @Qualifier("vq")
    private LinkedList<VocabularyMessage> vq;

    @RequestMapping(path = "/",method = RequestMethod.POST)
    public ResponseEntity<Object> putIntoQueue(@RequestBody Map params) {

        if(params.get("lang") == null || params.get("words") == null){
            throw new BusinessException(CustomMessageMap.SCRAWL_INVALID_PARAM);
        }

        int lang = Integer.parseInt(params.get("lang").toString());
        String words_str = params.get("words").toString();
        List<String> words = Arrays.asList(words_str.split("[,，]",-1));
        //去掉空白和太长太短的
        words = words.stream().map(w->w.trim()).collect(Collectors.toList());
        words = words.stream().filter(w->w.length()>2 && w.length() < 20).collect(Collectors.toList());

        // 放入队列
        words.forEach(w->vq.push(new VocabularyMessage(lang,w)));

        System.out.println("put a message "+ words_str);

        return new ResponseEntity("", HttpStatus.OK);
    }
}
