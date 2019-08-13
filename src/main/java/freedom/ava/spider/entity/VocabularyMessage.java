package freedom.ava.spider.entity;

public class VocabularyMessage {
    private int lang;
    private String spell;

    public VocabularyMessage(int lang,String spell){
        this.lang = lang;
        this.spell = spell;
    }

    public int getLang() {
        return lang;
    }

    public void setLang(int lang) {
        this.lang = lang;
    }

    public String getSpell() {
        return spell;
    }

    public void setSpell(String spell) {
        this.spell = spell;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof VocabularyMessage)) {
            return false;
        }
        VocabularyMessage msg = (VocabularyMessage) o;
        return msg.lang == lang &&
                msg.spell == spell;
    }
}
