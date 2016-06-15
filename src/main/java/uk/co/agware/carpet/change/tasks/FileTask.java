package uk.co.agware.carpet.change.tasks;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.agware.carpet.database.DatabaseConnector;
import uk.co.agware.carpet.exception.MagicCarpetException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 14/06/2016.
 */
public class FileTask extends DatabaseTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileTask.class);

    private String filePath;
    private String delimiter;

    public FileTask(DatabaseConnector databaseConnector, String taskName, int taskOrder, String filePath, String delimiter) {
        super(databaseConnector, taskName, taskOrder);
        this.filePath = filePath;
        this.delimiter = delimiter == null || "".equals(delimiter) ? ";" : delimiter;
    }

    @Override
    public boolean performTask() {
        try {
            String contents = new String(getFileContents());
            String[] statements = contents.split(delimiter);
            for(String s : statements){
                if(!databaseConnector.executeStatement(s.trim())){
                    return false;
                }
            }
        } catch (MagicCarpetException |IOException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    private byte[] getFileContents() throws IOException, MagicCarpetException {
        if(filePath.toLowerCase().startsWith("classpath:")){
            String filename = filePath.replace("classpath:", "");
            InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
            if(in != null) {
                return IOUtils.toByteArray(in);
            }
        }
        else {
            Path path = Paths.get(filePath);
            if(Files.exists(path)){
                return Files.readAllBytes(path);
            }
        }
        throw new MagicCarpetException("Unable to find file " +filePath);
    }
}
