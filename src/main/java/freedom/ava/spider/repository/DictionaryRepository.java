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

    @Select("select * from `explain` where word_id=#{word_id} and deleted=0")
    List<Explain> selectWordExplains(@Param("word_id") Integer word_id);

    @Select("select * from sentence where word_id=#{word_id} and deleted=0")
    List<Sentence> selectWordSentences(@Param("word_id") Integer word_id);

    @Select("select count(*) from book where deleted=0")
    Integer selectAllBookCount();

    @Select("select count(*) from book_word where book_id=#{id}")
    Integer countBookWords(@Param("id") Integer id);

    @Select("select count(*) from `explain` where word_id=#{id} and deleted=0")
    Integer countWordExplains(@Param("id") Integer id);

    @Select("select count(*) from sentence where explain_id=#{id} and deleted=0")
    Integer countExplainSentences(@Param("id") Integer id);

    @Insert("insert into word (lang,spell,pronounce,meaning,forms,deleted) values(#{lang},#{spell},#{pronounce},#{meaning},#{forms},0)")
    @SelectKey(statement="select @@identity", keyProperty="id", before=false, resultType=Integer.class)
    Integer insertWord(Word word);

    @Insert("insert into `explain` (word_id,`explain`,deleted) values(#{word_id},#{explain},0)")
    @SelectKey(statement="select @@identity", keyProperty="id", before=false, resultType=Integer.class)
    Integer insertExplain(Explain explain);

    @Insert("insert into sentence (word_id,explain_id,word,sentence,translation,deleted) values(#{word_id},#{explain_id},#{word},#{sentence},#{translation},0)")
    @SelectKey(statement="select @@identity", keyProperty="id", before=false, resultType=Integer.class)
    Integer insertSentence(Sentence sentence);

    @Insert("insert into user_book_word (user_book_id,word_id,deleted) values (#{user_book_id},#{word_id},0)")
    int addUserBookWords(@Param("user_book_id") int user_book_id, @Param("word_id") int word_id);
}
