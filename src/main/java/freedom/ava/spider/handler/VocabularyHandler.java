package freedom.ava.spider.handler;

import com.acxca.components.java.util.RandomUtil;
import freedom.ava.spider.config.Properties;
import freedom.ava.spider.entity.VocabularyMessage;
import freedom.ava.spider.entity.Word;
import freedom.ava.spider.service.SpiderService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;

/**
 * 爬单词
 */
@Component
public class VocabularyHandler {
    protected final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    @Qualifier("instant_q")
    private LinkedList<VocabularyMessage> instant_q;

    @Autowired
    @Qualifier("schedule_q")
    private LinkedList<VocabularyMessage> schedule_q;

    @Autowired
    private SpiderService spiderService;

    @Autowired
    private Properties properties;

    @PostConstruct
    public void start(){
        logger.info("Starting Spider Handler");
        new Thread(this::instantHandle).start();
        new Thread(this::scheduleHandle).start();
    }

    public void instantHandle() {
        while (true) {
            // 爬虫需要时间处理，如果这里用poll，在处理的过程当中，客户端又送过来一个同样的词，那么这个词又会被放进队列中。
            // 因此这里不能直接将词从队列中移除，需要在爬虫处理完以后再从队列移除
            VocabularyMessage msg = instant_q.peek();
            if (msg != null) {
                try {
                        logger.info("start grab " + msg.getSpell());
                        // 抓取可能会出异常
                        List<Word> words = spiderService.grabWord(msg.getLang(), msg.getSpell());

                } catch (Exception ex) {
                    //抓取异常，直接跳过，处理下一个
                    logger.error("word " + msg.getSpell() + " lang " + msg.getLang() + " grab fail!");
                    logger.error(ex);
                }
                finally {
                    instant_q.poll();
                }
            }

            // 这里要是不休眠，会导致后面整个线程卡主，没反应!!!
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }

    public void scheduleHandle(){
        Boolean did = false;
        int sleep = 0;
        while(true){
            did = false;
            VocabularyMessage msg = schedule_q.peek();
            if(msg != null) {
                try {
                    logger.info("get a message");
                    logger.info("start grab " + msg.getSpell());
                    // 抓取可能会出异常
                    List<Word> words = spiderService.grabWord(msg.getLang(), msg.getSpell());
                    did = true;

                    if (did) {
                        // TODO 暂时使用最简单的随机休眠策略
                        sleep = RandomUtil.getRandomInt(3 * 1000, 3 * 60 * 1000);
                        logger.info("sleep " + sleep);
                        try {
                            Thread.sleep(sleep);
                        } catch (Exception ex) {
                            logger.error(ex);
                        }
                    }
                } catch (Exception ex) {
                    //抓取异常，直接跳过，处理下一个
                    logger.error("word " + msg.getSpell() + " lang " + msg.getLang() + " grab fail!");
                    logger.error(ex);
                }
                finally {
                    schedule_q.poll();
                }
            }

            // 这里要是不休眠，会导致后面整个线程卡主，没反应!!!
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }
}
