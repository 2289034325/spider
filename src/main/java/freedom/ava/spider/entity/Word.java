package freedom.ava.spider.entity;


import java.util.Date;
import java.util.List;

public class Word
{
    private Integer id;
    private Integer lang;
    private String spell;
    private String pronounce;
    private String meaning;
    private String forms;
    private boolean deleted;
    private List<Explain> explains;
    private List<Sentence> sentences;

    private Date last_learn_time;
    private int answer_times;
    private int wrong_times;
    private int learn_phase;

    public String getForms() {
        return forms;
    }

    public void setForms(String forms) {
        this.forms = forms;
    }

    public Date getLast_learn_time() {
        return last_learn_time;
    }

    public void setLast_learn_time(Date last_learn_time) {
        this.last_learn_time = last_learn_time;
    }

    public int getAnswer_times() {
        return answer_times;
    }

    public void setAnswer_times(int answer_times) {
        this.answer_times = answer_times;
    }

    public int getWrong_times() {
        return wrong_times;
    }

    public void setWrong_times(int wrong_times) {
        this.wrong_times = wrong_times;
    }

    public int getLearn_phase() {
        return learn_phase;
    }

    public void setLearn_phase(int learn_phase) {
        this.learn_phase = learn_phase;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getPronounce() {
        return pronounce;
    }

    public void setPronounce(String pronounce) {
        this.pronounce = pronounce;
    }

    public List<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences = sentences;
    }

    public List<Explain> getExplains() {
        return explains;
    }

    public void setExplains(List<Explain> explains) {
        this.explains = explains;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getLang() {
        return lang;
    }

    public void setLang(Integer lang) {
        this.lang = lang;
    }

    public String getSpell() {
        return spell;
    }

    public void setSpell(String spell) {
        this.spell = spell;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
