package uk.co.agware.carpet;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.co.agware.carpet.change.Change;
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
import java.nio.file.Path;
import java.nio.file.Paths;
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
        if(filePath.toFile().exists()){
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
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList changeNodes = (NodeList) xPath.compile("changeList/change").evaluate(changeSetDoc, XPathConstants.NODESET);
            for (int i = 0; i < changeNodes.getLength(); i++) {
                Node change = changeNodes.item(i);
                String id = (String) xPath.compile("@id").evaluate(change, XPathConstants.STRING);
                String delimiter = (String) xPath.compile("@delimiter").evaluate(change, XPathConstants.STRING);
                String script = (String) xPath.compile("script").evaluate(change, XPathConstants.STRING);
                String file = (String) xPath.compile("file").evaluate(change, XPathConstants.STRING);

                String changeText;
                if(file != null && !file.equals("")){
                    changeText = new String(getFileContents(file));
                }
                else {
                    changeText = script;
                }

                if (delimiter != null && !delimiter.equals("")){
                    changes.add(new Change(id, changeText, delimiter));
                }
                else {
                    changes.add(new Change(id, changeText));
                }
            }
        } catch (SAXException | ParserConfigurationException | IOException | XPathExpressionException e) {
            LOGGER.error(e.getMessage(), e);
            throw new MagicCarpetException(e);
        }
    }

    private byte[] getFileContents(String filePath) throws IOException, MagicCarpetException {
        InputStream in = null;
        if(filePath.toLowerCase().startsWith("classpath:")){
            String filename = filePath.replace("classpath:", "");
            in = getClass().getClassLoader().getResourceAsStream(filename);
        }
        else {
            Path path = Paths.get(filePath);
            if(path.toFile().exists()){
                in = new FileInputStream(path.toFile());
            }
        }
        if(in != null){
            return IOUtils.toByteArray(in);
        }
        else {
            throw new MagicCarpetException("Unable to find file " +filePath);
        }
    }

    public boolean executeChanges() throws MagicCarpetException {
        if(devMode) return false;
        databaseConnector.checkChangeSetTable();
        if(changes != null && !changes.isEmpty()) {
            Collections.sort(changes);
            for(Change c : changes){
                if(c.isError()){
                    LOGGER.error(c.getErrorMessage());
                    throw new MagicCarpetException("Error found in change: " +c);
                }
            }
            return databaseConnector.executeChanges(changes);
        }
        return false;
    }

    public void run() throws MagicCarpetException {
        if(devMode) return;
        parseChanges();
        executeChanges();
    }
}
