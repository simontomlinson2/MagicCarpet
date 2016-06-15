package uk.co.agware.carpet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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

    private static final String CHANGE_SET_FILE = "src/test/files/ChangeSet.xml";

    private DatabaseConnector databaseConnector;
    private MagicCarpet magicCarpet;

    @Before
    public void buildMocks(){
        databaseConnector = Mockito.mock(DefaultDatabaseConnector.class);
        Mockito.when(databaseConnector.executeStatement(Mockito.anyString())).thenReturn(true);
        magicCarpet = new MagicCarpet(databaseConnector, false);
    }

    @Test
    public void testSetPath() throws MagicCarpetException {
        magicCarpet.setChangeSetFile(Paths.get(CHANGE_SET_FILE));
        magicCarpet.parseChanges();
        Assert.assertEquals(2, magicCarpet.getChanges().size());
    }

    @Test(expected = MagicCarpetException.class)
    public void testSetPathDoesntExist() throws MagicCarpetException {
        magicCarpet.setChangeSetFile(Paths.get("this/file/doesnt/exist"));
    }

    @Test
    public void testSetInputStream() throws FileNotFoundException, MagicCarpetException {
        magicCarpet.setChangeSetFile(new FileInputStream(new File(CHANGE_SET_FILE)));
        magicCarpet.parseChanges();
        Assert.assertEquals(2, magicCarpet.getChanges().size());
    }

    @Test
    public void testExecuteChanges() throws FileNotFoundException, MagicCarpetException {
        magicCarpet.setChangeSetFile(new FileInputStream(new File(CHANGE_SET_FILE)));
        magicCarpet.parseChanges();
        Assert.assertTrue(magicCarpet.executeChanges());
        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        Mockito.verify(databaseConnector, Mockito.times(5)).executeStatement(statements.capture());
        Assert.assertEquals(5, statements.getAllValues().size());
        Assert.assertTrue(statements.getAllValues().contains("create table test(version integer, test date)"));
        Assert.assertTrue(statements.getAllValues().contains("alter table test add column another varchar(64)"));
        Assert.assertTrue(statements.getAllValues().contains("create table second(version varchar(64))"));
        Assert.assertTrue(statements.getAllValues().contains("SELECT * FROM Table"));
        Assert.assertTrue(statements.getAllValues().contains("SELECT * FROM Other_Table"));
    }

    @Test
    public void testExecuteFailureDoesRollBack() {
        DatabaseConnector databaseConnector = Mockito.mock(DatabaseConnector.class);
        Mockito.when(databaseConnector.executeStatement(Mockito.anyString())).thenReturn(false);
        MagicCarpet magicCarpet = new MagicCarpet(databaseConnector);

        Exception e = null; // Need to catch the exception to make sure it was thrown but also that the methods were called on the databaseConnector
        try {
            magicCarpet.run();
        } catch (MagicCarpetException e1) {
            e = e1;
        }
        Assert.assertNotNull(e);
        Mockito.verify(databaseConnector, Mockito.times(1)).rollBack();
        Mockito.verify(databaseConnector, Mockito.times(1)).close();
    }

    @Test
    public void testRun() throws FileNotFoundException, MagicCarpetException {
        magicCarpet.run();
        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        Mockito.verify(databaseConnector, Mockito.times(2)).executeStatement(statements.capture());
        Assert.assertEquals(2, statements.getAllValues().size());
        Assert.assertTrue(statements.getAllValues().contains("SELECT * FROM Classpath"));
        Assert.assertTrue(statements.getAllValues().contains("SELECT * FROM class_path"));
    }

    @Test
    public void testClasspathChangeFile() throws FileNotFoundException, SQLException, MagicCarpetException {
        magicCarpet.parseChanges();
        Assert.assertEquals(1, magicCarpet.getChanges().size());
        Assert.assertEquals(1, magicCarpet.getChanges().get(0).getTasks().size());
        Assert.assertTrue(magicCarpet.executeChanges());
        Mockito.verify(databaseConnector, Mockito.times(2)).executeStatement(Mockito.anyString());
    }

    @Test
    public void testDevMode() throws FileNotFoundException, MagicCarpetException {
        magicCarpet.setChangeSetFile(new FileInputStream(new File(CHANGE_SET_FILE)));
        magicCarpet.setDevMode(true);
        magicCarpet.run();
        Assert.assertFalse(magicCarpet.executeChanges());
        Mockito.verify(databaseConnector, Mockito.times(0)).executeStatement(Mockito.anyString());
    }

    @Test
    public void testConstructorWithoutFlag() throws MagicCarpetException {
        MagicCarpet magicCarpet = new MagicCarpet(databaseConnector);
        magicCarpet.parseChanges();
        Assert.assertTrue(magicCarpet.executeChanges());
    }
}
