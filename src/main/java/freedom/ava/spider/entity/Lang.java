package freedom.ava.spider.entity;

public enum Lang {
    EN(1),
    JP(2), 
    KR(3),
    FR(4);

    private int index;

    Lang(int idx) {
        this.index = idx;
    }

    public int getIndex() {
        return index;
    }

}
