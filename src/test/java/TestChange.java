import org.junit.Assert;
import org.junit.Test;
import uk.co.agware.carpet.change.Change;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 2/05/2016.
 */
public class TestChange {

    @Test
    public void testConstructor(){
        Change change = new Change("1", "SELECT * FROM Table");
        Assert.assertEquals(change.getId(), "1");
        Assert.assertEquals(change.getInputList().length, 1);
        Assert.assertEquals(change.getInputList()[0], "SELECT * FROM Table");
    }

    @Test
    public void testDefaultDelimiter(){
        Change change = new Change("1", "SELECT * FROM Table; SELECT * FROM Table2");
        Assert.assertEquals(change.getInputList().length, 2);
    }

    @Test
    public void testDelimiter(){
        Change change = new Change("1", "SELECT * FROM Table | SELECT * FROM Table2", "|");
        Assert.assertEquals(change.getInputList().length, 2);
        Change change2 = new Change("1", "SELECT * FROM Table \\ SELECT * FROM Table2", "\\");
        Assert.assertEquals(change2.getInputList().length, 2);
    }

    @Test
    public void testSortSuccess(){
        Change change1 = new Change("1", "SELECT * FROM Table");
        Change change2 = new Change("2", "SELECT * FROM Table");
        List<Change> changes = new ArrayList<>();
        changes.add(change1);
        changes.add(change2);
        Collections.sort(changes);
        for(Change c: changes){
            Assert.assertFalse(c.isError());
        }
    }

    @Test
    public void testSortFail(){
        Change change1 = new Change("1", "SELECT * FROM Table");
        Change change2 = new Change("1", "SELECT * FROM Table");
        Collections.sort(Arrays.asList(change1, change2));
        Assert.assertTrue(change1.isError() || change2.isError());
        Assert.assertTrue("Version number 1 detected twice".equals(change1.getErrorMessage()) || "Version number 1 detected twice".equals(change2.getErrorMessage()));
    }

    @Test
    public void testOrdering(){
        List<Change> changes = new ArrayList<>();
        changes.add(new Change("1.0.0", "SELECT * FROM Table"));
        changes.add(new Change("1.1.0", "SELECT * FROM Table"));
        changes.add(new Change("1.0.1", "SELECT * FROM Table"));
        changes.add(new Change("1.1.2", "SELECT * FROM Table"));
        changes.add(new Change("1.0.7.2", "SELECT * FROM Table"));
        changes.add(new Change("1.0.7", "SELECT * FROM Table"));
        changes.add(new Change("2", "SELECT * FROM Table"));
        Collections.sort(changes);
        Assert.assertTrue(changes.get(0).getId().equals("1.0.0"));
        Assert.assertTrue(changes.get(1).getId().equals("1.0.1"));
        Assert.assertTrue(changes.get(2).getId().equals("1.0.7"));
        Assert.assertTrue(changes.get(3).getId().equals("1.0.7.2"));
        Assert.assertTrue(changes.get(4).getId().equals("1.1.0"));
        Assert.assertTrue(changes.get(5).getId().equals("1.1.2"));
        Assert.assertTrue(changes.get(6).getId().equals("2"));
    }
}
