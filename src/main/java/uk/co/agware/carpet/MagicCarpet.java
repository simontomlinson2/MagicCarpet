package uk.co.agware.carpet;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.co.agware.carpet.change.Change;
import uk.co.agware.carpet.change.tasks.FileTask;
import uk.co.agware.carpet.change.tasks.ScriptTask;
import uk.co.agware.carpet.change.tasks.Task;
import uk.co.agware.carpet.database.DatabaseConnector;
import uk.co.agware.carpet.exception.MagicCarpetException;
import uk.co.agware.carpet.util.FileUtil;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 26/02/2016.
 */
public class MagicCarpet {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagicCarpet.class);

    private List<Change> changes = null;
    private DatabaseConnector databaseConnector;
    private InputStream fileInput;
    private boolean devMode;
    private XPath xPath;

    public MagicCarpet(DatabaseConnector databaseConnector) {
        this.databaseConnector = databaseConnector;
        this.devMode = false;
    }

    public MagicCarpet(DatabaseConnector databaseConnector, boolean devMode) {
        this.databaseConnector = databaseConnector;
        this.devMode = devMode;
    }

    public void setChangeSetFile(InputStream inputStream){
        fileInput = inputStream;
    }

    public void setChangeSetFile(Path filePath) throws MagicCarpetException {
        if(Files.exists(filePath)){
            try {
                fileInput = new FileInputStream(filePath.toString());
            } catch (FileNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
                throw new MagicCarpetException("Unable to find file: " +filePath.toString());
            }
        }
        else {
            LOGGER.error("File {} does not exist", filePath.toString());
            throw new MagicCarpetException("Unable to find file: " +filePath.toString());
        }
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public List<Change> getChanges() {
        return changes;
    }

    public void parseChanges() throws MagicCarpetException {
        if(devMode) return;

        if(fileInput == null) {
            fileInput = getClass().getClassLoader().getResourceAsStream("ChangeSet.xml");
        }

        if(fileInput == null) {
            LOGGER.error("No ChangeSet.xml found");
            throw new MagicCarpetException("No ChangeSet.xml found");
        }

        changes = new ArrayList<>();
        try {
            Document changeSetDoc = FileUtil.byteArrayToDocument(IOUtils.toByteArray(fileInput));
            fileInput.close();
            xPath = XPathFactory.newInstance().newXPath();
            NodeList changeNodes = (NodeList) xPath.compile("changeList/change").evaluate(changeSetDoc, XPathConstants.NODESET);
            for (int i = 0; i < changeNodes.getLength(); i++) {
                Node change = changeNodes.item(i);
                String version = (String) xPath.compile("@version").evaluate(change, XPathConstants.STRING);
                NodeList taskNodes = (NodeList) xPath.compile("task").evaluate(change, XPathConstants.NODESET);
                List<Task> tasks = buildVersionTasks(taskNodes);
                changes.add(new Change(version, tasks));
            }
        } catch (SAXException | ParserConfigurationException | IOException | XPathExpressionException e) {
            LOGGER.error(e.getMessage(), e);
            throw new MagicCarpetException(e);
        }
    }

    private List<Task> buildVersionTasks(NodeList taskNodes) throws MagicCarpetException {
        List<Task> tasks = new ArrayList<>(taskNodes.getLength());
        try {
            for (int j = 0; j < taskNodes.getLength(); j++) {
                Node task = taskNodes.item(j);
                String name = (String) xPath.compile("@name").evaluate(task, XPathConstants.STRING);
                String orderString = (String) xPath.compile("@order").evaluate(task, XPathConstants.STRING);
                int order = orderString == null || "".equals(orderString) ? Integer.MAX_VALUE : Integer.parseInt(orderString);
                String script = (String) xPath.compile("script").evaluate(task, XPathConstants.STRING);
                String file = (String) xPath.compile("file").evaluate(task, XPathConstants.STRING);

                if (file != null && !file.equals("")) {
                    String delimiter = (String) xPath.compile("file/@delimiter").evaluate(task, XPathConstants.STRING);
                    tasks.add(new FileTask(databaseConnector, name, order, file, delimiter));
                } else {
                    String delimiter = (String) xPath.compile("script/@delimiter").evaluate(task, XPathConstants.STRING);
                    tasks.add(new ScriptTask(databaseConnector, name, order, script, delimiter));
                }
            }
        } catch (XPathExpressionException e) {
            LOGGER.error(e.getMessage(), e);
            throw new MagicCarpetException(e.getMessage(), e);
        }
        return tasks;
    }

    public boolean executeChanges() throws MagicCarpetException {
        if(devMode) {
            LOGGER.info("MagicCarpet set to Dev Mode, changes not being implemented");
            return false;
        }
        databaseConnector.checkChangeSetTable();
        if(changes != null && !changes.isEmpty()) {
            Collections.sort(changes);
            for(Change c : changes){
                Collections.sort(c.getTasks());
                for(Task t : c.getTasks()){
                    if(!databaseConnector.changeExists(c.getVersion(), t.getTaskName())){
                        if(t.performTask()){
                            databaseConnector.insertChange(c.getVersion(), t.getTaskName());
                        }
                        else {
                            databaseConnector.rollBack();
                            databaseConnector.close();
                            throw new MagicCarpetException(String.format("Error while inserting Task %s for Change Version %s, see the log for additional details", t.getTaskName(), c.getVersion()));
                        }
                    }
                }
            }
        }
        return true;
    }

    public void run() throws MagicCarpetException {
        if(devMode) {
            LOGGER.info("MagicCarpet set to Dev Mode, changes not being implemented");
            return;
        }
        parseChanges();
        executeChanges();
    }
}
