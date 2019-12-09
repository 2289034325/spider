package freedom.ava.spider.service;

import freedom.ava.spider.config.Properties;
import freedom.ava.spider.entity.Explain;
import freedom.ava.spider.entity.Lang;
import freedom.ava.spider.entity.Sentence;
import freedom.ava.spider.entity.Word;
import freedom.ava.spider.util.BusinessException;
import freedom.ava.spider.util.CustomMessageMap;
import freedom.ava.spider.util.RandomUtil;
import freedom.ava.spider.util.RegularExpressionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SpiderService {
    @Autowired
    private Properties properties;

    @Autowired
    private WebDriver phantomJSDriver;

    @Autowired
    @Qualifier("varBag")
    private HashMap<String, Object> varBag;

    /**
     * 理论上有两条线程会使用到该方法
     * 一个是管理员用户的即时爬取，一个是管理后台的批量爬取
     *
     * @param lang
     * @param word
     * @return
     */
    synchronized public List<Word> grabWord(int lang, String word) {
        String requestUrl = "";
        List<Word> words = new ArrayList<>();

        try {
            word = URLEncoder.encode(word, "utf-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("encode error");
            e.printStackTrace();
        }

        if (lang == Lang.EN.getIndex()) {
            requestUrl = String.format(properties.getHjen(), word);
        } else if (lang == Lang.JP.getIndex()) {
            requestUrl = String.format(properties.getHjjp(), word);
        } else if (lang == Lang.KR.getIndex()) {
            requestUrl = String.format(properties.getHjkr(), word);
        } else if (lang == Lang.FR.getIndex()) {
            requestUrl = String.format(properties.getHjfr(), word);
        }
        String html = getHtml(requestUrl);

        //解析html
        if(lang == Lang.EN.getIndex()) {
            Word w = analyzeHtml_hj_en(html);
            if(w != null){
                words.add(w);
            }
        }
        else if(lang == Lang.FR.getIndex()){
            Word w = analyzeHtml_hj_fr(html);
            if(w != null){
                words.add(w);
            }
        }
        else if(lang == Lang.KR.getIndex()){
            Word w = analyzeHtml_hj_kr(html);
            if(w != null){
                words.add(w);
            }
        }
        else if(lang == Lang.JP.getIndex()) {
            words = analyzeHtml_hj_jp(html);
        }

        return words;
    }

    private String getHtml(String requestUrl) {
        phantomJSDriver.get(requestUrl);
        String html = phantomJSDriver.getPageSource();

        long lastGrabTime = 0;
        if (varBag.get("lastGrabTime") != null) {
            lastGrabTime = (long) varBag.get("lastGrabTime");
        }
        long spanTime = new Date().getTime() - lastGrabTime;
        // 匿名session有效期为30分钟，需要重新发送请求，才能查询到
        if (spanTime > 29 * 60 * 1000) {
            try {
                //随机睡眠3秒
                Thread.sleep(RandomUtil.getRandomInt(500, 3 * 1000));
            } catch (Exception ex) {
                System.out.println(ex);
            }
            phantomJSDriver.get(requestUrl);
            html = phantomJSDriver.getPageSource();
        }
        varBag.put("lastGrabTime", new Date().getTime());

        System.out.println("get response");

        return html;
    }

    private Word analyzeHtml_hj_en(String html) {
        Document doc = Jsoup.parse(html);

        //解析单词（查询的如果不是一般时态，沪江会返回正常时态的单词，例如查询does，返回的是do
        String word_text = "";
        try {
            Element div = doc.getElementsByClass("word-text").first();
            word_text = div.children().first().text().trim();

            System.out.println("find spell");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (word_text.isEmpty()) {
            throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_SPELL);
        }


        //解析发音
        String pronounce = "";
        try {
            Elements eles = doc.getElementsByClass("pronounce-value-us");
            pronounce = eles.get(0).text().trim().replace("[", "").replace("]", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (pronounce.isEmpty()) {
            System.out.println("find no pronounce");
        } else {
            System.out.println("find pronounce");
        }

        //解析意思
        String meaning = "";
        try {
            Element ele = doc.getElementsByClass("simple").first();
            for (Element p : ele.children()) {
                Elements sps = p.children();
                // 有词性
                if (sps.size() == 2) {
                    if (meaning.isEmpty()) {
                        meaning = sps.get(0).text().trim() + " " + sps.get(1).text().trim();
                    } else {
                        meaning += "\r\n" + sps.get(0).text().trim() + " " + sps.get(1).text().trim();
                    }
                }
                // 没词性
                else {
                    if (meaning.isEmpty()) {
                        meaning = sps.get(0).text().trim();
                    } else {
                        meaning += "\r\n" + sps.get(0).text().trim();
                    }
                }

                System.out.println("find meaning");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (meaning.isEmpty()) {
            throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_MEANING);
        }

        //解析释义
        List<Explain> exps = new ArrayList<>();
        try {
            Elements ex_sections = doc.getElementsByClass("detail-groups");
            if (ex_sections.size() == 0) {
                throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_EXPLAIN);
            }
            Element ex_section = ex_sections.get(0);
            Elements dls = ex_section.getElementsByTag("dl");
            for (Element dl : dls) {
                // 为了不让一个出错就导致整个抓取失败，不抛出异常，继续解析下一个
                try {
                    // 每个释义条目可能有不同的发音
                    String prn = "";
                    try {
                        Element dt = dl.getElementsByTag("dt").first();
                        prn = dt.children().first().text().trim().replace("/", "");
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }

                    Elements dds = dl.getElementsByTag("dd");
                    for (Element dd : dds) {
                        // 为了不让一个出错就导致整个抓取失败，不抛出异常，继续解析下一个
                        try {
                            Element h3 = dd.child(0);
                            Explain exp = new Explain();
                            exp.setPronounce(prn);
                            exp.setExplain(h3.text().trim());

                            Element ul = dd.getElementsByTag("ul").first();
                            List<Sentence> sts = new ArrayList<>();
                            Elements lis = ul.children();
                            for (Element li : lis) {
                                String sentence = li.child(0).text().trim();
                                // 这里不将原形用作默认词形，测验时可尽量选择有词形的例句
                                // 因为即使将原形用作默认词形也不一定对
                                String word_form = "";
                                if (li.child(0).children().size() > 0) {
                                    word_form = li.child(0).child(0).text().trim();
                                }

                                String translation = li.child(1).text().trim();

                                Sentence s = new Sentence();
                                s.setSentence(sentence);
                                s.setTranslation(translation);
                                s.setWord(word_form);

                                sts.add(s);
                            }

                            // 没有例句的释义就不保存了
                            if(sts.size()>0) {
                                exp.setSentences(sts);
                                exps.add(exp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (exps.size() == 0) {
            System.out.println("find exps");
        } else {
            System.out.println("find no exps");
        }

        //词形变化（包括原型和所有变形，用来检索，因为有时候用户检索的并不是单词原形）
        List<String> forms = new ArrayList<>();
        forms.add(word_text);
        try {
            Elements uls = doc.getElementsByClass("inflections-items");
            if (uls.size() > 0) {
                Elements lis = uls.get(0).children();
                for (Element li : lis) {
                    forms.add(li.child(1).text().trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 再结合例句中所有的词形
        exps.forEach(e -> e.getSentences().forEach(s -> {
            if(!s.getWord().isEmpty()) {
                if (!forms.contains(s.getWord())) {
                    forms.add(s.getWord());
                }
            }
        }));

        // 为了将来检索提高效率，将所有词形包上括号[]
        // 如果不加[]，例如forms为abc,bcd,acd，使用 like 时，bc 将被判定为符合条件
        // 加上[] 后[abc],[bcd],[acd]，用[bc]去检索，就不会出现这个问题
        // foreach 不会改变元素!!!
        // forms.forEach(f->f="["+f+"]");
        List<String> newForms = forms.stream().map(l -> "[" + l + "]").collect(Collectors.toList());
        String forms_str = String.join(",", newForms);

        System.out.println("finish grab");

        Word w = new Word();
        w.setLang(Lang.EN.getIndex());
        w.setSpell(word_text);
        w.setPronounce(pronounce);
        w.setMeaning(meaning);
        w.setForms(forms_str);
        w.setExplains(exps);

        return w;
    }

    private Word analyzeHtml_hj_fr(String html) {
        Document doc = Jsoup.parse(html);

        //解析单词（查询的如果不是一般时态，沪江会返回正常时态的单词，例如查询does，返回的是do
        String word_text = "";
        try {
            Element div = doc.getElementsByClass("word-text").first();
            word_text = div.children().first().text().trim();

            System.out.println("find spell");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (word_text.isEmpty()) {
            throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_SPELL);
        }


        //解析发音
        String pronounce = "";
        try {
            Elements eles = doc.getElementsByClass("pronounces");
            pronounce = eles.get(0).text().trim().replace("[", "").replace("]", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (pronounce.isEmpty()) {
            System.out.println("find no pronounce");
        } else {
            System.out.println("find pronounce");
        }

        //解析意思
        String meaning = "";
        try {
            Element div = doc.getElementsByClass("simple").first();
            for (Element p : div.children()) {
                if (meaning.isEmpty()) {
                    meaning = p.text().trim();
                } else {
                    if (meaning.endsWith("\r\n")) {
                        meaning += p.text().trim();
                    } else {
                        meaning += "\r\n" + p.text().trim();
                    }
                }

                System.out.println("find meaning");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (meaning.isEmpty()) {
            throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_MEANING);
        }

        //解析释义
        List<Explain> exps = new ArrayList<>();
        try {
            Elements ex_sections = doc.getElementsByClass("detail-groups");
            if (ex_sections.size() == 0) {
                throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_EXPLAIN);
            }
            Element ex_section = ex_sections.get(0);
            Elements dls = ex_section.getElementsByTag("dl");
            for (Element dl : dls) {
                // 为了不让一个出错就导致整个抓取失败，不抛出异常，继续解析下一个
                try {
                    Elements dds = dl.getElementsByTag("dd");
                    for (Element dd : dds) {
                        // 为了不让一个出错就导致整个抓取失败，不抛出异常，继续解析下一个
                        try {
                            Element h3 = dd.child(0);
                            Explain exp = new Explain();
                            exp.setPronounce(pronounce);
                            exp.setExplain(h3.text().trim());

                            Element ul = dd.getElementsByTag("ul").first();
                            List<Sentence> sts = new ArrayList<>();
                            Elements lis = ul.children();
                            for (Element li : lis) {
                                String sentence = li.child(0).text().trim();
                                // 这里不将原形用作默认词形，测验时可尽量选择有词形的例句
                                // 因为即使将原形用作默认词形也不一定对
                                String word_form = "";
                                if (li.child(0).children().size() > 0) {
                                    word_form = li.child(0).child(0).text().trim();
                                }

                                String translation = li.child(1).text().trim();

                                Sentence s = new Sentence();
                                s.setSentence(sentence);
                                s.setTranslation(translation);
                                s.setWord(word_form);

                                sts.add(s);
                            }

                            // 没有例句的释义就不保存了
                            if(sts.size()>0) {
                                exp.setSentences(sts);
                                exps.add(exp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (exps.size() == 0) {
            System.out.println("find exps");
        } else {
            System.out.println("find no exps");
        }

        //词形变化（包括原型和所有变形，用来检索，因为有时候用户检索的并不是单词原形）
        List<String> forms = new ArrayList<>();
        forms.add(word_text);
        try {
            Elements uls = doc.getElementsByClass("inflections-items");
            if (uls.size() > 0) {
                Elements lis = uls.get(0).children();
                for (Element li : lis) {
                    forms.add(li.child(1).text().trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 再结合例句中所有的词形
        exps.forEach(e -> e.getSentences().forEach(s -> {
            if(!s.getWord().isEmpty()) {
                if (!forms.contains(s.getWord())) {
                    forms.add(s.getWord());
                }
            }
        }));

        // 为了将来检索提高效率，将所有词形包上括号[]
        // 如果不加[]，例如forms为abc,bcd,acd，使用 like 时，bc 将被判定为符合条件
        // 加上[] 后[abc],[bcd],[acd]，用[bc]去检索，就不会出现这个问题
        // foreach 不会改变元素!!!
        // forms.forEach(f->f="["+f+"]");
        List<String> newForms = forms.stream().map(l -> "[" + l + "]").collect(Collectors.toList());
        String forms_str = String.join(",", newForms);

        System.out.println("finish grab");

        Word w = new Word();
        w.setLang(Lang.FR.getIndex());
        w.setSpell(word_text);
        w.setPronounce(pronounce);
        w.setMeaning(meaning);
        w.setForms(forms_str);
        w.setExplains(exps);

        return w;
    }

    private Word analyzeHtml_hj_kr(String html) {
        Document doc = Jsoup.parse(html);

        //解析单词（查询的如果不是一般时态，沪江会返回正常时态的单词，例如查询does，返回的是do
        String word_text = "";
        try {
            Element div = doc.getElementsByClass("word-text").first();
            word_text = div.children().first().text().trim();

            System.out.println("find spell");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (word_text.isEmpty()) {
            throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_SPELL);
        }


        //解析发音
        String pronounce = "";
        try {
            Elements eles = doc.getElementsByClass("pronounces");
            pronounce = eles.get(0).text().trim().replace("[", "").replace("]", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (pronounce.isEmpty()) {
            System.out.println("find no pronounce");
        } else {
            System.out.println("find pronounce");
        }

        //解析意思
        String meaning = "";
        try {
            Element div = doc.getElementsByClass("simple").first();
            Element ul = div.getElementsByTag("ul").first();
            for (Element li : ul.children()) {
                if (meaning.isEmpty()) {
                    meaning = li.text().trim();
                } else {
                    if (meaning.endsWith("\r\n")) {
                        meaning += li.text().trim();
                    } else {
                        meaning += "\r\n" + li.text().trim();
                    }
                }

                System.out.println("find meaning");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (meaning.isEmpty()) {
            throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_MEANING);
        }

        //解析释义
        List<Explain> exps = new ArrayList<>();
        try {
            Elements ex_sections = doc.getElementsByClass("detail-groups");
            if (ex_sections.size() == 0) {
                throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_EXPLAIN);
            }
            Element ex_section = ex_sections.get(0);
            Elements dls = ex_section.getElementsByTag("dl");
            for (Element dl : dls) {
                // 为了不让一个出错就导致整个抓取失败，不抛出异常，继续解析下一个
                try {
                    Elements dds = dl.getElementsByTag("dd");
                    for (Element dd : dds) {
                        // 为了不让一个出错就导致整个抓取失败，不抛出异常，继续解析下一个
                        try {
                            Element h3 = dd.child(0);
                            Explain exp = new Explain();
                            exp.setPronounce(pronounce);
                            exp.setExplain(h3.text().trim());

                            Element ul = dd.getElementsByTag("ul").first();
                            List<Sentence> sts = new ArrayList<>();
                            Elements lis = ul.children();
                            for (Element li : lis) {
                                String sentence = li.child(0).text().trim();
                                // 这里不将原形用作默认词形，测验时可尽量选择有词形的例句
                                // 因为即使将原形用作默认词形也不一定对
                                String word_form = "";
                                if (li.child(0).children().size() > 0) {
                                    word_form = li.child(0).child(0).text().trim();
                                }

                                String translation = li.child(1).text().trim();

                                Sentence s = new Sentence();
                                s.setSentence(sentence);
                                s.setTranslation(translation);
                                s.setWord(word_form);

                                sts.add(s);
                            }

                            // 没有例句的释义就不保存了
                            if(sts.size()>0) {
                                exp.setSentences(sts);
                                exps.add(exp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (exps.size() == 0) {
            System.out.println("find exps");
        } else {
            System.out.println("find no exps");
        }

        //词形变化（包括原型和所有变形，用来检索，因为有时候用户检索的并不是单词原形）
        List<String> forms = new ArrayList<>();
        forms.add(word_text);
        try {
            Elements uls = doc.getElementsByClass("inflections-items");
            if (uls.size() > 0) {
                Elements lis = uls.get(0).children();
                for (Element li : lis) {
                    forms.add(li.child(1).text().trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 再结合例句中所有的词形
        exps.forEach(e -> e.getSentences().forEach(s -> {
            if(!s.getWord().isEmpty()) {
                if (!forms.contains(s.getWord())) {
                    forms.add(s.getWord());
                }
            }
        }));

        // 为了将来检索提高效率，将所有词形包上括号[]
        // 如果不加[]，例如forms为abc,bcd,acd，使用 like 时，bc 将被判定为符合条件
        // 加上[] 后[abc],[bcd],[acd]，用[bc]去检索，就不会出现这个问题
        // foreach 不会改变元素!!!
        // forms.forEach(f->f="["+f+"]");
        List<String> newForms = forms.stream().map(l -> "[" + l + "]").collect(Collectors.toList());
        String forms_str = String.join(",", newForms);

        System.out.println("finish grab");

        Word w = new Word();
        w.setLang(Lang.KR.getIndex());
        w.setSpell(word_text);
        w.setPronounce(pronounce);
        w.setMeaning(meaning);
        w.setForms(forms_str);
        w.setExplains(exps);

        return w;
    }

    // 日语检索结果有可能有多个不同词（同形不同音，或者同音不同形）
    private List<Word> analyzeHtml_hj_jp(String html) {
        List<Word> words = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        Elements pnls = doc.getElementsByClass("word-details-pane");
        for(Element pnl : pnls){
            Word w = analyzeHtml_hj_jp_one(pnl);
            if(w != null){
                Word exw = words.stream().filter(wi->wi.getSpell().equals(w.getSpell())).findAny().orElse(null);
                if(exw == null) {
                    words.add(w);
                }
                // 合并同形词（词形一样的算同一个词有多个音）
                else{
                    String prn = exw.getPronounce()+","+w.getPronounce();
                    String mng;
                    // 没有多个发音，表明尚未合并。合并前需要把本身的发音放到意思前面
                    if(exw.getPronounce().contains(",")) {
                        mng = exw.getMeaning() + "\r\n\r\n" + w.getPronounce() + "\r\n" + w.getMeaning();
                    }
                    else{
                        mng = exw.getPronounce() + "\r\n" + exw.getMeaning() + "\r\n\r\n" + w.getPronounce() + "\r\n" + w.getMeaning();
                    }

                    exw.setPronounce(prn);
                    exw.setMeaning(mng);
                    exw.getExplains().addAll(w.getExplains());
                }
            }
        }

        return words;
    }

    @Nullable
    private Word analyzeHtml_hj_jp_one(Element wordBlock){
        try {
            //解析单词（查询的如果不是一般时态，沪江会返回正常时态的单词，例如查询does，返回的是do
            String word_text = "";

            try {
                Element div = wordBlock.getElementsByClass("word-text").first();
                word_text = div.children().first().text().trim();

                System.out.println("find spell");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (word_text.isEmpty()) {
                throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_SPELL);
            }


            //解析发音
            String pronounce = "";
            try {
                Element div = wordBlock.getElementsByClass("pronounces").first();
                pronounce = div.children().first().text().trim().replace("[", "").replace("]", "");
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (pronounce.isEmpty()) {
                System.out.println("find no pronounce");
            } else {
                System.out.println("find pronounce");
            }

            //解析意思
            String meaning = "";
            try {
                Element div = wordBlock.getElementsByClass("simple").first();
                Element ul = div.getElementsByTag("ul").first();
                for (Element li : ul.children()) {
                    if (meaning.isEmpty()) {
                        meaning = li.text().trim();
                    } else {
                        if (meaning.endsWith("\r\n")) {
                            meaning += li.text().trim();
                        } else {
                            meaning += "\r\n" + li.text().trim();
                        }
                    }

                    System.out.println("find meaning");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (meaning.isEmpty()) {
                throw new BusinessException(CustomMessageMap.SCRAWL_FORMAT_WRONG_NO_MEANING);
            }

            //解析释义
            List<Explain> exps = new ArrayList<>();
            try {
                Element ex_section = wordBlock.getElementsByClass("detail-groups").first();
                Elements dls = ex_section.getElementsByTag("dl");
                for (Element dl : dls) {
                    // 为了不让一个出错就导致整个抓取失败，不抛出异常，继续解析下一个
                    try {
                        Elements dds = dl.getElementsByTag("dd");
                        for (Element dd : dds) {
                            // 为了不让一个出错就导致整个抓取失败，不抛出异常，继续解析下一个
                            try {
                                Element h3 = dd.child(0);
                                Explain exp = new Explain();
                                exp.setPronounce(pronounce);
                                exp.setExplain(h3.text().trim());

                                Element ul = dd.getElementsByTag("ul").first();
                                List<Sentence> sts = new ArrayList<>();
                                Elements lis = ul.children();
                                for (Element li : lis) {
                                    String sentence = li.child(0).text().trim();
                                    // 不设默认词形，测验的时候可尽量选择有词形的例句
                                    String word_form = "";
                                    if (li.child(0).children().size() > 0) {
                                        word_form = li.child(0).child(0).text().trim();
                                    }

                                    String translation = li.child(1).text().trim();

                                    Sentence s = new Sentence();
                                    s.setSentence(sentence);
                                    s.setTranslation(translation);
                                    s.setWord(word_form);

                                    sts.add(s);
                                }

                                // 没有例句的释义就不保存了
                                if (sts.size() > 0) {
                                    exp.setSentences(sts);
                                    exps.add(exp);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (exps.size() == 0) {
                System.out.println("find exps");
            } else {
                System.out.println("find no exps");
            }

            //词形变化（包括原型和所有变形，用来检索，因为有时候用户检索的并不是单词原形）
            List<String> forms = new ArrayList<>();
            forms.add(word_text);
            // 日语检索页面没有词形变化区域
            // 再结合例句中所有的词形
            exps.forEach(e -> e.getSentences().forEach(s -> {
                if (!s.getWord().isEmpty()) {
                    if (!forms.contains(s.getWord())) {
                        forms.add(s.getWord());
                    }
                }
            }));

            // 为了将来检索提高效率，将所有词形包上括号[]
            // 如果不加[]，例如forms为abc,bcd,acd，使用 like 时，bc 将被判定为符合条件
            // 加上[] 后[abc],[bcd],[acd]，用[bc]去检索，就不会出现这个问题
            // foreach 不会改变元素!!!
            // forms.forEach(f->f="["+f+"]");
            List<String> newForms = forms.stream().map(l -> "[" + l + "]").collect(Collectors.toList());
            String forms_str = String.join(",", newForms);

            System.out.println("finish grab");

            Word w = new Word();
            w.setLang(Lang.JP.getIndex());
            w.setSpell(word_text);
            w.setPronounce(pronounce);
            w.setMeaning(meaning);
            w.setForms(forms_str);
            w.setExplains(exps);

            return w;
        }
        catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }
}
