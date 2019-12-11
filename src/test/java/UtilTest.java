import freedom.ava.spider.entity.VocabularyMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class UtilTest {
    protected final Log logger = LogFactory.getLog(this.getClass());

    @Test
    public void VocabularyMessageTest()
    {
        LinkedList<VocabularyMessage> vq = new LinkedList<>();
        VocabularyMessage m1 = new VocabularyMessage(1,"cat");
        VocabularyMessage m2 = new VocabularyMessage(1,"dog");

        vq.add(m1);
        vq.add(m2);

        VocabularyMessage tm1 = new VocabularyMessage(1,"cat");
        VocabularyMessage tm2 = new VocabularyMessage(2,"cat");

        assert vq.contains(tm1);
        assert !vq.contains(tm2);
    }

    @Test
    public void LinkedListTest()
    {
        LinkedList<Integer> linkedList = new LinkedList<>();

        linkedList.add(0);
        linkedList.add(1);

        logger.info(linkedList.poll());
    }

    @Test
    public void ListForeachTest()
    {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list = list.stream().map(l-> "["+l+"]").collect(Collectors.toList());
        String str = String.join(",",list);

        logger.info(str);
    }
}
