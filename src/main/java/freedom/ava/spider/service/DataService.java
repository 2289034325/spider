package freedom.ava.spider.service;

import freedom.ava.spider.entity.Explain;
import freedom.ava.spider.entity.Sentence;
import freedom.ava.spider.entity.Word;
import freedom.ava.spider.repository.DictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Repository
public class DataService {

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Transactional
    public void saveWord(Word w){
        //保存入库
        dictionaryRepository.insertWord(w);
        for(Explain e: w.getExplains()){
            e.setWord_id(w.getId());
            dictionaryRepository.insertExplain(e);
            for(Sentence s:e.getSentences()){
                s.setWord_id(w.getId());
                s.setExplain_id(e.getId());
                dictionaryRepository.insertSentence(s);
            }
        }

        //放到用户词书 TODO 暂时的做法，以后要删除
        Map<String,Integer> bk = dictionaryRepository.selectDefaultBook(1,w.getLang());
        if(bk != null){
            int book_id = bk.get("book_id");
            dictionaryRepository.insertBookWord(book_id,w.getId());
            dictionaryRepository.updateBookWordCount(book_id);
        }
    }
}
