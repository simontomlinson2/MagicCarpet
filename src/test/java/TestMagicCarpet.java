import org.junit.*;
import org.junit.Test;
import org.mockito.Mockito;
import uk.co.agware.carpet.MagicCarpet;
import uk.co.agware.carpet.database.DatabaseConnector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 2/05/2016.
 */
public class TestMagicCarpet {

    private DatabaseConnector databaseConnector;
    private MagicCarpet magicCarpet;

    @Before
    @SuppressWarnings("unchecked")
    public void buildMocks(){
        databaseConnector = Mockito.mock(DatabaseConnector.class);
        Mockito.when(databaseConnector.executeChanges(Mockito.anyList())).thenReturn(true);
        magicCarpet = new MagicCarpet(databaseConnector);
    }

    @Test
    public void testSetPath(){
        magicCarpet.setChangeSetFile(Paths.get("src/test/java/ChangeSet.xml"));
        magicCarpet.parseChanges();
        Assert.assertFalse(magicCarpet.isError());
        Assert.assertEquals(magicCarpet.getChanges().size(), 2);
    }

    @Test
    public void testSetInputStream() throws FileNotFoundException {
        magicCarpet.setChangeSetFile(new FileInputStream(new File("src/test/java/ChangeSet.xml")));
        magicCarpet.parseChanges();
        Assert.assertFalse(magicCarpet.isError());
        Assert.assertEquals(magicCarpet.getChanges().size(), 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecuteChanges() throws FileNotFoundException {
        magicCarpet.setChangeSetFile(new FileInputStream(new File("src/test/java/ChangeSet.xml")));
        magicCarpet.parseChanges();
        Assert.assertTrue(magicCarpet.executeChanges());
        Mockito.verify(databaseConnector).executeChanges(Mockito.anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDevMode() throws FileNotFoundException {
        magicCarpet.setChangeSetFile(new FileInputStream(new File("src/test/java/ChangeSet.xml")));
        magicCarpet.setDevMode(true);
        magicCarpet.parseChanges();
        Assert.assertFalse(magicCarpet.executeChanges());
        Mockito.verify(databaseConnector, Mockito.times(0)).executeChanges(Mockito.anyList());
    }
}
