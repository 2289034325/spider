package freedom.ava.spider.service;

import freedom.ava.spider.config.Properties;
import freedom.ava.spider.entity.Explain;
import freedom.ava.spider.entity.Sentence;
import freedom.ava.spider.entity.Word;
import freedom.ava.spider.util.BusinessException;
import freedom.ava.spider.util.CustomMessageMap;
import freedom.ava.spider.util.RandomUtil;
import freedom.ava.spider.util.RegularExpressionUtils;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SpiderService {
    @Autowired
    private Properties properties;

    @Autowired
    private PhantomJSDriver phantomJSDriver;

    @Autowired
    @Qualifier("varBag")
    private HashMap<String,Object> varBag;

    public Word grabWord(int lang, String word){
        String requestUrl = "";
        if(lang == 1){
            requestUrl = String.format(properties.getHjen(),word);
        }
        else if(lang == 2){
            requestUrl = String.format(properties.getHjjp(),word);
        }
        else if(lang == 3){
            requestUrl = String.format(properties.getHjkr(),word);
        }
        else if(lang == 4){
            requestUrl = String.format(properties.getHjfr(),word);
        }

        phantomJSDriver.get(requestUrl);
        String html = phantomJSDriver.getPageSource();

        long lastGrabTime = 0;
        if(varBag.get("lastGrabTime")!= null) {
            lastGrabTime = (long) varBag.get("lastGrabTime");
        }
        long spanTime = new Date().getTime()-lastGrabTime;
        // 匿名session有效期为30分钟，需要重新发送请求，才能查询到
        if(spanTime>29*60*1000)
        {
            try {
                //随机睡眠3秒
                Thread.sleep( RandomUtil.getRandomInt(500,3*1000));
            }
            catch (Exception ex){
                System.out.println(ex);
            }
            phantomJSDriver.get(requestUrl);
            html = phantomJSDriver.getPageSource();
        }
        varBag.put("lastGrabTime",new Date().getTime());

        System.out.println("get response");

        //解析html
        Word w = analyzeHtml(lang,html);

        return w;
    }

    private Word analyzeHtml(int lang,String html) {

        //解析单词（查询的如果不是一般时态，沪江会返回正常时态的单词，例如查询does，返回的是do
        String word_text = "";
        Pattern pt_word_text = Pattern.compile("\\<div class\\=\"word-text\"\\>([\\s\\S]*?)\\<h2\\>(.*)\\</h2>([\\s\\S]*?)\\</div\\>");
//        Matcher mc_word_text = pt_word_text.matcher(html);
//        while (mc_word_text.find()) {
//            word_text = mc_word_text.group(2);
//        }
        Matcher mc_word_text = RegularExpressionUtils.createMatcherWithTimeout(
                html, pt_word_text, 3000);
        try {
            while (mc_word_text.find()) {
                word_text = mc_word_text.group(2);
            }
        }
        catch (Exception ex){
            System.out.println(ex);
        }

        if(word_text.isEmpty()){
            throw new BusinessException(CustomMessageMap.SCRAWL_NONE);
        }

        System.out.println("find spell");

        //解析发音
        String pronounce = "";
        Pattern pt_pronounce = Pattern.compile("\\<span class\\=\"pronounce-value-us\"\\>\\[(.*)\\]\\</span\\>");
//        Matcher mc_pronounce = pt_pronounce.matcher(html);
//        while (mc_pronounce.find()) {
//            pronounce = mc_pronounce.group(1);
//        }
        Matcher mc_pronounce = RegularExpressionUtils.createMatcherWithTimeout(
                html, pt_pronounce, 3000);
        try {
            while (mc_pronounce.find()) {
                pronounce = mc_pronounce.group(1);
            }
        }
        catch (Exception ex){
            System.out.println(ex);
        }

        if(pronounce.isEmpty()){
            throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG);
        }

        System.out.println("find pronounce");

        //解析意思
        String meaning = "";
        Pattern pt_meaning = Pattern.compile("<p>[\\s\\S]*?<span>([\\s\\S]*?)</span>[\\s\\S]*?<span class\\=\"simple-definition\">([\\s\\S]*?)</span>[\\s\\S]*?</p>");
        //Matcher.find 方法可能会卡死线程，需要嵌入超时机制，防止线程卡死!!!
//        Matcher mc_meaning = pt_meaning.matcher(html);
        Matcher mc_meaning = RegularExpressionUtils.createMatcherWithTimeout(
                html, pt_meaning, 3000);
        try {
            while (mc_meaning.find()) {
                if (meaning.isEmpty()) {
                    meaning = mc_meaning.group(1).trim() + " " + mc_meaning.group(2).trim();
                } else {
                    meaning += "\r\n" + mc_meaning.group(1).trim() + " " + mc_meaning.group(2).trim();
                }
                System.out.println("find a meaning");
            }
        }
        catch (Exception ex){
            System.out.println(ex);
        }
        if (meaning.isEmpty()) {
            throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG);
        }

        List<Explain> exps = new ArrayList<>();
        //解析释义组
        List<String> ls_groups = new ArrayList<>();
        Pattern pt_groups = Pattern.compile("\\<dd[\\s\\S]*?\\>[\\s\\S]*?\\<h3\\>+[\\s\\S]*?\\<\\/dd\\>");
//        Matcher mc_groups = pt_groups.matcher(html);
//        while (mc_groups.find()) {
//            ls_groups.add(mc_groups.group());
//        }
        Matcher mc_groups = RegularExpressionUtils.createMatcherWithTimeout(
                html, pt_groups, 3000);
        try {
            while (mc_groups.find()) {
                ls_groups.add(mc_groups.group());
            }
        }
        catch (Exception ex){
            System.out.println(ex);
        }

        //解析释义
        Pattern pt_explain = Pattern.compile("\\<h3\\>([\\s\\S]*?)\\<p\\>([\\s\\S]*?)\\<\\/p\\>([\\s\\S]*?)\\<\\/h3\\>");
        Pattern pt_sentence_block = Pattern.compile("<li>([\\s\\S]*?)</li>");
        Pattern pt_sentence = Pattern.compile("<p class=\"def-sentence-from\">([\\s\\S]*?)<span([\\s\\S]*?)></span>([\\s\\S]*?)</p>");
        Pattern pt_word = Pattern.compile("<mark(.*)>(.*)</mark>");
        Pattern pt_translation = Pattern.compile("<p class=\"def-sentence-to\">([\\s\\S]*?)</p>");
        List<Explain> ls_explain = new ArrayList<>();
        List<Sentence> ls_sentence;
        String explain = "";
        //多个不同释义
        for (String exp : ls_groups) {
            ls_sentence = new ArrayList<>();

            //释义
//            Matcher mc_explain = pt_explain.matcher(exp);
//            while (mc_explain.find()) {
//                explain = mc_explain.group(2).trim().replace("\r","").replace("\n","").replace(" ","");
//            }
            Matcher mc_explain = RegularExpressionUtils.createMatcherWithTimeout(exp, pt_explain, 3000);
            try {
                while (mc_explain.find()) {
                    explain = mc_explain.group(2).trim().replace("\r","").replace("\n","").replace(" ","");
                    System.out.println("find a explain "+ explain);
                }
            }
            catch (Exception ex){
                System.out.println(ex);
            }

            //多个句子
//            Matcher mc_sentence_block = pt_sentence_block.matcher(exp);
            Matcher mc_sentence_block = RegularExpressionUtils.createMatcherWithTimeout(
                    exp, pt_sentence_block, 10*1000);
            try {
                while (mc_sentence_block.find()) {
//                    Matcher mc_word = pt_word.matcher(mc_sentence_block.group());
                    Matcher mc_word = RegularExpressionUtils.createMatcherWithTimeout(mc_sentence_block.group(), pt_word, 3000);
//                    Matcher mc_sentence = pt_sentence.matcher(mc_sentence_block.group());
                    Matcher mc_sentence = RegularExpressionUtils.createMatcherWithTimeout(mc_sentence_block.group(), pt_sentence, 3000);
//                    Matcher mc_translation = pt_translation.matcher(mc_sentence_block.group());
                    Matcher mc_translation = RegularExpressionUtils.createMatcherWithTimeout(mc_sentence_block.group(), pt_translation, 3000);

                    String word_shape = "";
                    String mark = "";
                    String sentence = "";
                    String translation = "";
                    try {
                        while (mc_word.find()) {
                            mark = mc_word.group();
                            word_shape = mc_word.group(2);
                        }
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                    try {
                        while (mc_sentence.find()) {
                            sentence = mc_sentence.group(1).replace(mark,word_shape).trim();
                        }
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                    try {
                        while (mc_translation.find()) {
                            translation = mc_translation.group(1).trim();
                        }
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }

                    System.out.println("find a sentence "+sentence);

                    Sentence s = new Sentence();
                    s.setWord(word_shape);
                    s.setSentence(sentence);
                    s.setTranslation(translation);
                    ls_sentence.add(s);
                }
            }
            catch (Exception ex){
                System.out.println(ex);
            }

            Explain e = new Explain();
            e.setExplain(explain);
            e.setSentences(ls_sentence);

            ls_explain.add(e);
        }

        System.out.println("finish grab");

        Word w = new Word();
        w.setLang(lang);
        w.setSpell(word_text);
        w.setPronounce(pronounce);
        w.setMeaning(meaning);
        w.setExplains(ls_explain);

        return w;
    }
}
