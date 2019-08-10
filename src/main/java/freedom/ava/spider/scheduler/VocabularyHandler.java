package freedom.ava.spider.scheduler;

import freedom.ava.spider.entity.Explain;
import freedom.ava.spider.entity.Sentence;
import freedom.ava.spider.entity.VocabularyMessage;
import freedom.ava.spider.entity.Word;
import freedom.ava.spider.repository.DictionaryRepository;
import freedom.ava.spider.service.DataService;
import freedom.ava.spider.service.SpiderService;
import freedom.ava.spider.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * 爬单词
 */
@Component
public class VocabularyHandler {

    @Autowired
    @Qualifier("vq")
    private LinkedList<VocabularyMessage> vq;

    @Autowired
    private SpiderService spiderService;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private DataService dataService;

    @PostConstruct
    public void start(){
        System.out.println("Starting Spider Handler");
        new Thread(this::handle).start();
    }

    public void handle(){
        Boolean did = false;
        int sleep = 0;
        while(true){
            did = false;
            VocabularyMessage msg = vq.poll();
            if(msg != null){
                System.out.println("get a message");
                // 检查是否已经存在
                List<Word> wo = dictionaryRepository.selectWord2(msg.getLang(),msg.getSpell());
                if(wo.size()==0){
                    Word w = null;
                    try {
                        System.out.println("start grab "+msg.getSpell());
                        // 抓取可能会出异常
                        w = spiderService.grabWord(msg.getLang(), msg.getSpell());
                    }
                    catch (Exception ex){
                        //抓取异常，直接跳过，处理下一个
                        System.out.println("word "+msg.getSpell()+" lang "+ msg.getLang() +" grab fail!");
                        System.out.println(ex);
                    }
                    did = true;
                    if(w != null) {
                        // 爬到的词形可能跟输入的词形不一致，需要再检查一遍
                        wo = dictionaryRepository.selectWord2(msg.getLang(), w.getSpell());
                        if (wo.size() == 0) {
                            dataService.saveWord(w);
                        }
                    }
                }
                if(did) {
                    //随机休眠3分钟（至少3秒钟）
                    sleep = RandomUtil.getRandomInt(3*1000, 3 * 60 * 1000);
                    System.out.println("sleep "+sleep);
                    try {
                        Thread.sleep(sleep);
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
                else{
                    continue;
                }
            }

            // 这里要是不休眠，会导致后面整个线程卡主，没反应!!!
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }
}
