package freedom.ava.spider.api;

import freedom.ava.spider.entity.VocabularyMessage;
import freedom.ava.spider.entity.Word;
import freedom.ava.spider.repository.DictionaryRepository;
import freedom.ava.spider.service.DataService;
import freedom.ava.spider.service.SpiderService;
import freedom.ava.spider.util.BusinessException;
import freedom.ava.spider.util.CustomMessageMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 爬单词
 */

@RestController
@RequestMapping(path = "vocabulary",produces= MediaType.APPLICATION_JSON_VALUE)
public class VocabularyController {

    @Autowired
    @Qualifier("instant_q")
    private LinkedList<VocabularyMessage> instant_q;

    @Autowired
    @Qualifier("schedule_q")
    private LinkedList<VocabularyMessage> schedule_q;

    @Autowired
    private SpiderService spiderService;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private DataService dataService;

    @RequestMapping(path = "/instant/{lang}/{form}",method = RequestMethod.GET)
    public ResponseEntity<Object> instantGrab(@PathVariable("lang") int lang,@PathVariable("form") String form) {

        form = form.trim();
        if(form.length()<2 || form.length() > 20){
            throw new BusinessException(CustomMessageMap.SCRAWL_INVALID_PARAM);
        }

        VocabularyMessage msg = new VocabularyMessage(lang,form);
        if(!instant_q.contains(msg)) {
            instant_q.add(msg);
            System.out.println("put a message "+ form);
        }

        return new ResponseEntity("", HttpStatus.OK);
    }

    @RequestMapping(path = "/queue",method = RequestMethod.POST)
    public ResponseEntity<Object> scheduleGrab(@RequestBody Map params) {

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
        words.forEach(w->{
            VocabularyMessage msg = new VocabularyMessage(lang,w);
            if(!schedule_q.contains(msg)) {
                schedule_q.add(msg);
                System.out.println("put a message "+ w);
            }
        });

        return new ResponseEntity("", HttpStatus.OK);
    }
}
