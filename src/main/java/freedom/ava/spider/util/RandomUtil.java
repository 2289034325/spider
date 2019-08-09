package freedom.ava.spider.util;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil {
    public static int getRandomInt(int min,int max){
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
