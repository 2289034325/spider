package freedom.ava.spider.repository;

import freedom.ava.spider.entity.Explain;
import freedom.ava.spider.entity.Sentence;
import freedom.ava.spider.entity.Word;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface DictionaryRepository {

    @Select("select * from word where lang=#{lang} and forms like '%' #{form} '%' and deleted=0")
    List<Word> selectWordsByForm(@Param("lang") Integer lang,@Param("form") String form);

    @Select("select * from word where lang=#{lang} and spell=#{spell} and deleted=0")
    List<Word> selectWordsBySpell(@Param("lang") Integer lang, @Param("spell") String spell);

    @Insert("insert into word (lang,spell,pronounce,meaning,forms,deleted) values(#{lang},#{spell},#{pronounce},#{meaning},#{forms},0)")
    @SelectKey(statement="select @@identity", keyProperty="id", before=false, resultType=Integer.class)
    Integer insertWord(Word word);

    @Insert("insert into `explain` (word_id,`explain`,deleted) values(#{word_id},#{explain},0)")
    @SelectKey(statement="select @@identity", keyProperty="id", before=false, resultType=Integer.class)
    Integer insertExplain(Explain explain);

    @Insert("insert into sentence (word_id,explain_id,word,sentence,translation,deleted) values(#{word_id},#{explain_id},#{word},#{sentence},#{translation},0)")
    @SelectKey(statement="select @@identity", keyProperty="id", before=false, resultType=Integer.class)
    Integer insertSentence(Sentence sentence);
}
