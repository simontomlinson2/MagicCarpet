import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.co.agware.carpet.MagicCarpet;
import uk.co.agware.carpet.database.DatabaseConnector;
import uk.co.agware.carpet.database.DefaultDatabaseConnector;
import uk.co.agware.carpet.exception.MagicCarpetException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 2/05/2016.
 */
public class TestMagicCarpet {

    private DatabaseConnector databaseConnector;
    private MagicCarpet magicCarpet;

    @Before
    @SuppressWarnings("unchecked")
    public void buildMocks(){
        databaseConnector = Mockito.mock(DefaultDatabaseConnector.class);
        Mockito.when(databaseConnector.executeChanges(Mockito.anyList())).thenReturn(true);
        magicCarpet = new MagicCarpet(databaseConnector);
    }

    @Test
    public void testSetPath() throws MagicCarpetException {
        magicCarpet.setChangeSetFile(Paths.get("src/test/java/ChangeSet.xml"));
        magicCarpet.parseChanges();
        Assert.assertEquals(magicCarpet.getChanges().size(), 2);
    }

    @Test
    public void testSetInputStream() throws FileNotFoundException, MagicCarpetException {
        magicCarpet.setChangeSetFile(new FileInputStream(new File("src/test/java/ChangeSet.xml")));
        magicCarpet.parseChanges();
        Assert.assertEquals(magicCarpet.getChanges().size(), 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecuteChanges() throws FileNotFoundException, MagicCarpetException {
        magicCarpet.setChangeSetFile(new FileInputStream(new File("src/test/java/ChangeSet.xml")));
        magicCarpet.parseChanges();
        Assert.assertTrue(magicCarpet.executeChanges());
        Mockito.verify(databaseConnector).executeChanges(Mockito.anyList());
    }

    @Test
    public void testClasspathChangeFile() throws FileNotFoundException, SQLException, MagicCarpetException {
        magicCarpet.parseChanges();
        Assert.assertEquals(magicCarpet.getChanges().size(), 1);
        Assert.assertEquals(magicCarpet.getChanges().get(0).getInputList().length, 1);
        Assert.assertEquals(magicCarpet.getChanges().get(0).getInputList()[0], "SELECT * FROM Classpath");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDevMode() throws FileNotFoundException, MagicCarpetException {
        magicCarpet.setChangeSetFile(new FileInputStream(new File("src/test/java/ChangeSet.xml")));
        magicCarpet.setDevMode(true);
        magicCarpet.parseChanges();
        Assert.assertFalse(magicCarpet.executeChanges());
        Mockito.verify(databaseConnector, Mockito.times(0)).executeChanges(Mockito.anyList());
    }
}
