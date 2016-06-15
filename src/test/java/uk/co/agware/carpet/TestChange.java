package uk.co.agware.carpet;

import org.junit.Assert;
import org.junit.Test;
import uk.co.agware.carpet.change.Change;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 2/05/2016.
 */
public class TestChange {

    @Test
    public void testEquals(){
        Assert.assertTrue(new Change("1", null).equals(new Change("1", null)));
        Assert.assertFalse(new Change("1", null).equals(new Change("2", null)));
    }

    @Test
    public void testOrdering(){
        List<Change> changes = new ArrayList<>();
        changes.add(new Change("1.0.0", null));
        changes.add(new Change("1.1.0", null));
        changes.add(new Change("1.0.1", null));
        changes.add(new Change("1.1.2", null));
        changes.add(new Change("1.0.7.2", null));
        changes.add(new Change("1.0.7", null));
        changes.add(new Change("2", null));
        Collections.sort(changes);
        Assert.assertTrue(changes.get(0).getVersion().equals("1.0.0"));
        Assert.assertTrue(changes.get(1).getVersion().equals("1.0.1"));
        Assert.assertTrue(changes.get(2).getVersion().equals("1.0.7"));
        Assert.assertTrue(changes.get(3).getVersion().equals("1.0.7.2"));
        Assert.assertTrue(changes.get(4).getVersion().equals("1.1.0"));
        Assert.assertTrue(changes.get(5).getVersion().equals("1.1.2"));
        Assert.assertTrue(changes.get(6).getVersion().equals("2"));
    }
}
